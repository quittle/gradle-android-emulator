package com.quittle.androidemulator;

import com.android.build.gradle.BaseExtension;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

import com.android.build.gradle.internal.tasks.DeviceProviderInstrumentTestTask;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.AbstractExecTask;
import org.gradle.api.tasks.Exec;
import org.gradle.api.tasks.TaskInstantiationException;

@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class AndroidEmulatorPlugin implements Plugin<Project> {
    public static final String ENSURE_ANDROID_EMULATOR_PERMISSIONS_TASK_NAME = "ensureAndroidEmulatorPermissions";
    public static final String INSTALL_ANDROID_EMULATOR_TASK_NAME = "installAndroidEmulator";
    public static final String INSTALL_ANDROID_EMULATOR_SYSTEM_IMAGE_TASK_NAME = "installAndroidEmulatorSystemImage";
    public static final String CREATE_ANDROID_EMULATOR_TASK_NAME = "createAndroidEmulator";
    public static final String START_ANDROID_EMULATOR_TASK_NAME = "startAndroidEmulator";
    public static final String WAIT_FOR_ANDROID_EMULATOR_TASK_NAME = "waitForAndroidEmulator";
    public static final String STOP_ANDROID_EMULATOR_TASK_NAME = "stopAndroidEmulator";

    @Override
    public void apply(final Project project) {
        final AndroidEmulatorExtension extension =
                project.getExtensions().create("androidEmulator", AndroidEmulatorExtension.class);

        project.afterEvaluate(p -> {
            final BaseExtension androidExtension = p.getExtensions().findByType(BaseExtension.class);
            if (androidExtension == null) {
                throw new TaskInstantiationException("Android extension not found. Make sure the Android plugin is applied");
            }

            final EmulatorConfiguration emulatorConfiguration = new EmulatorConfiguration(project, androidExtension, extension);

            if (emulatorConfiguration.getEnableForAndroidTests()) {
                setUpAndroidTests(p);
            }

            createEnsurePermissionsTask(p, emulatorConfiguration);
            createInstallEmulatorTask(p, emulatorConfiguration);
            createInstallEmulatorSystemImageTask(p, emulatorConfiguration);
            createCreateEmulatorTask(p, emulatorConfiguration);
            createStartStopEmulatorTasks(p, emulatorConfiguration);
            createWaitForEmulatorTask(p, emulatorConfiguration);
        });
    }

    private static void setUpAndroidTests(final Project project) {
        project.getTasks().withType(
                DeviceProviderInstrumentTestTask.class, task -> {
                    task.dependsOn(WAIT_FOR_ANDROID_EMULATOR_TASK_NAME);
                    task.finalizedBy(STOP_ANDROID_EMULATOR_TASK_NAME);
                });
    }

    private static boolean ensureFileIsExecutable(final File file) {
        return file.canExecute() || file.setExecutable(true);
    }

    private static void createEnsurePermissionsTask(final Project project, final EmulatorConfiguration emulatorConfiguration) {
        project.getTasks().create(ENSURE_ANDROID_EMULATOR_PERMISSIONS_TASK_NAME, task -> {
            task.doFirst(t -> {
                if (!ensureFileIsExecutable(emulatorConfiguration.getSdkManager())) {
                    throw new RuntimeException("Unable to make SDK Manager executable");
                }

                if (!ensureFileIsExecutable(emulatorConfiguration.getAvdManager())) {
                    throw new RuntimeException("Unable to make Android Virtual Device manager executable");
                }

                if (!ensureFileIsExecutable(emulatorConfiguration.getAdb())) {
                    throw new RuntimeException(("Unable to make ADB executable"));
                }
            });
        });
    }

    private static void createInstallEmulatorTask(final Project project, final EmulatorConfiguration emulatorConfiguration) {
        createExecTask(project, emulatorConfiguration, INSTALL_ANDROID_EMULATOR_TASK_NAME, exec -> {
                    exec.setExecutable(emulatorConfiguration.getSdkManager());
                    exec.setArgs(l("emulator"));
                    exec.setStandardInput(buildStandardInLines("y"));
                    exec.getOutputs().dir(new File(emulatorConfiguration.getSdkRoot(), "emulator"));

                    exec.dependsOn(ENSURE_ANDROID_EMULATOR_PERMISSIONS_TASK_NAME);
                    exec.doLast(t -> {
                        if (!emulatorConfiguration.getEmulator().setExecutable(true)) {
                            throw new RuntimeException("Unable to make android emulator executable");
                        }
                    });
                });
    }

    private static void createInstallEmulatorSystemImageTask(final Project project, final EmulatorConfiguration emulatorConfiguration) {
        createExecTask(project, emulatorConfiguration, INSTALL_ANDROID_EMULATOR_SYSTEM_IMAGE_TASK_NAME, exec -> {
                    exec.setExecutable(emulatorConfiguration.getSdkManager());
                    exec.setArgs(l(emulatorConfiguration.getSystemImagePackageName()));
                    exec.setStandardInput(buildStandardInLines("y"));
                    exec.getOutputs().dir(emulatorConfiguration.sdkFile("system-images", emulatorConfiguration.getAndroidVersion(), emulatorConfiguration.getFlavor(), emulatorConfiguration.getAbi()));
                    exec.getOutputs().file(emulatorConfiguration.sdkFile("system-images", emulatorConfiguration.getAndroidVersion(), emulatorConfiguration.getFlavor(), emulatorConfiguration.getAbi(), "system.img"));
                    exec.dependsOn(ENSURE_ANDROID_EMULATOR_PERMISSIONS_TASK_NAME);
                });
    }

    private static void createCreateEmulatorTask(final Project project, final EmulatorConfiguration emulatorConfiguration) {
        final File avdRoot = emulatorConfiguration.getAvdRoot();
        final String emulatorName = emulatorConfiguration.getEmulatorName();
        createExecTask(project, emulatorConfiguration, CREATE_ANDROID_EMULATOR_TASK_NAME, exec -> {
                    exec.setExecutable(emulatorConfiguration.getAvdManager());
                    exec.setArgs(l("create", "avd", "--name", emulatorName, "--package", emulatorConfiguration.getSystemImagePackageName(), "--force"));
                    exec.setStandardInput(buildStandardInLines("no"));
                    exec.getOutputs().dir(new File(avdRoot, emulatorName + ".avd"));
                    exec.getOutputs().file(new File(avdRoot, emulatorName + ".ini"));

                    exec.dependsOn(INSTALL_ANDROID_EMULATOR_SYSTEM_IMAGE_TASK_NAME, ENSURE_ANDROID_EMULATOR_PERMISSIONS_TASK_NAME);
                });
    }

    private static void createStartStopEmulatorTasks(final Project project, final EmulatorConfiguration emulatorConfiguration) {
        final List<String> command = new ArrayList<>();
        command.add(emulatorConfiguration.getEmulator().getAbsolutePath());
        command.add("@" + emulatorConfiguration.getEmulatorName());
        command.add("-shell");
        if (emulatorConfiguration.getHeadless()) {
            command.add("-no-skin");
            command.add("-no-audio");
            command.add("-no-window");
        }
        final ProcessBuilder pb = new ProcessBuilder(command.toArray(new String[0]));
        pb.environment().putAll(emulatorConfiguration.getEnvironmentVariableMap());
        pb.inheritIO();

        final AtomicReference<Process> process = new AtomicReference<>();

        project.getTasks().create(START_ANDROID_EMULATOR_TASK_NAME, task -> {
            task.doFirst(t -> {
                project.getLogger().debug("Starting emulator with command {} {}", pb.environment(), pb.command());
                try {
                    process.set(pb.start());
                    Runtime.getRuntime().addShutdownHook(new Thread(() -> process.getAndUpdate(DESTROY_AND_REPLACE_WITH_NULL)));
                } catch (final IOException e) {
                    throw new RuntimeException("Emulator failed to start successfully", e);
                }
            });

            task.dependsOn(ENSURE_ANDROID_EMULATOR_PERMISSIONS_TASK_NAME, INSTALL_ANDROID_EMULATOR_TASK_NAME, CREATE_ANDROID_EMULATOR_TASK_NAME);
            task.finalizedBy(STOP_ANDROID_EMULATOR_TASK_NAME);
        });

        project.getTasks().create(STOP_ANDROID_EMULATOR_TASK_NAME, task -> {
            task.doFirst(t -> process.getAndUpdate(DESTROY_AND_REPLACE_WITH_NULL));

            task.dependsOn(ENSURE_ANDROID_EMULATOR_PERMISSIONS_TASK_NAME, START_ANDROID_EMULATOR_TASK_NAME);
            task.mustRunAfter(WAIT_FOR_ANDROID_EMULATOR_TASK_NAME);
        });
    }

    private static void createWaitForEmulatorTask(final Project project, final EmulatorConfiguration emulatorConfiguration) {
        createExecTask(project, emulatorConfiguration, WAIT_FOR_ANDROID_EMULATOR_TASK_NAME, exec -> {
            exec.setExecutable(emulatorConfiguration.getAdb());
            exec.setArgs(l("wait-for-device", "shell", "while $(exit $(getprop sys.boot_completed)) ; do sleep 1; done;"));
//            exec.setArgs(l("wait-for-device", "shell", "sleep 1000000; while [ -z $(getprop sys.boot_completed) ]; do sleep 1; done;"));

            exec.dependsOn(ENSURE_ANDROID_EMULATOR_PERMISSIONS_TASK_NAME, START_ANDROID_EMULATOR_TASK_NAME);
        });
    }

    private static UnaryOperator<Process> DESTROY_AND_REPLACE_WITH_NULL = process -> {
        if (process != null) {
            process.destroyForcibly();
        }

        return null;
    };

    @SuppressWarnings("unchecked")
    private static Task createExecTask(final Project project,
                                       final EmulatorConfiguration emulatorConfiguration,
                                       final String name,
                                       final Action<AbstractExecTask<Exec>> configure) {
        return project.getTasks().create(name, Exec.class, (final Exec task) -> {
            task.environment(emulatorConfiguration.getEnvironmentVariableMap());

            configure.execute(task);

            task.getInputs().property("environmentMaps", emulatorConfiguration.getEnvironmentVariableMap());
            task.getInputs().property("command", task.getCommandLine());
        });
    }

    @SafeVarargs
    private static <T> List<T> l(T... arr) {
        final List<T> ret = new ArrayList<>();
        for (final T v : arr) {
            ret.add(v);
        }
        return ret;
    }

    private static InputStream buildStandardInLines(String... lines) {
        final String string = String.join(System.lineSeparator(), lines);
        final byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
        return new ByteArrayInputStream(bytes);
    }
}