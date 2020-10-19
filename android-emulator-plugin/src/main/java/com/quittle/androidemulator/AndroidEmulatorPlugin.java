package com.quittle.androidemulator;

import com.android.build.gradle.BaseExtension;
import com.android.build.gradle.internal.tasks.DeviceProviderInstrumentTestTask;
import org.apache.commons.io.IOUtils;
import org.gradle.api.*;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.AbstractExecTask;
import org.gradle.api.tasks.Exec;
import org.gradle.api.tasks.TaskExecutionException;
import org.gradle.api.tasks.TaskInstantiationException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class AndroidEmulatorPlugin implements Plugin<Project> {
    public static final String ENSURE_BASE_SDK_PERMISSIONS_TASK_NAME = "ensureBaseSdkPermissionsForAndroidEmulatorPlugin";
    public static final String ENSURE_INSTALLED_SDK_PERMISSIONS_TASK_NAME = "ensureInstalledSdkPermissionsForAndroidEmulatorPlugin";
    public static final String ADD_ADDITIONAL_SDK_REPOSITORIES_TASK_NAME = "addAdditionalSdkRepositoriesForAndroidEmulatorPlugin";
    public static final String INSTALL_SDK_DEPENDENCIES_TASK_NAME = "installSdkDependenciesForAndroidEmulatorPlugin";
    public static final String INSTALL_ANDROID_EMULATOR_SYSTEM_IMAGE_TASK_NAME = "installAndroidEmulatorSystemImageForAndroidEmulatorPlugin";
    public static final String CREATE_ANDROID_EMULATOR_TASK_NAME = "createAndroidEmulator";
    public static final String START_ANDROID_EMULATOR_TASK_NAME = "startAndroidEmulator";
    public static final String WAIT_FOR_ANDROID_EMULATOR_TASK_NAME = "waitForAndroidEmulator";
    public static final String STOP_ANDROID_EMULATOR_TASK_NAME = "stopAndroidEmulator";
    /**
     * This is matching adb output lines. E.g.
     * <pre>{@code
     * emulator-5554       device
     * 192.168.1.2:42839   device
     * }</pre>
     */
    private static final Pattern ADB_OUTPUT_PATTERN = Pattern.compile("([\\w\\d\\-:\\.]+)\\s+([\\d\\w\\-]+)");
    private static final UnaryOperator<Process> DESTROY_AND_REPLACE_WITH_NULL = process -> {
        if (process != null) {
            process.destroy();
            process.destroyForcibly();
        }

        return null;
    };

    private static void setUpAndroidTests(final Project project) {
        project.getTasks().withType(
                DeviceProviderInstrumentTestTask.class, task -> {
                    task.dependsOn(WAIT_FOR_ANDROID_EMULATOR_TASK_NAME);
                    task.finalizedBy(STOP_ANDROID_EMULATOR_TASK_NAME);
                });
    }

    /**
     * Ensures given file is executable, and if not, tries promote file to be executable
     *
     * @return true, if file is executable
     */
    private static boolean ensureFileIsExecutable(final File file) {
        // First check if file is executable. In most environments sdk binaries are already executable.
        // In some environments your process is not an owner of the file and can't change permission.
        // For example docker or CI
        return file.canExecute() || file.setExecutable(true);
    }

    private static void createEnsurePermissionsTasks(final Project project, final EmulatorConfiguration emulatorConfiguration) {
        project.getTasks().create(ENSURE_BASE_SDK_PERMISSIONS_TASK_NAME, task -> {
            task.doFirst(t -> {
                if (!ensureFileIsExecutable(emulatorConfiguration.getSdkManager())) {
                    throw new RuntimeException("Unable to make SDK Manager executable");
                }

                if (!ensureFileIsExecutable(emulatorConfiguration.getAdb())) {
                    throw new RuntimeException(("Unable to make ADB executable"));
                }
            });
        });

        project.getTasks().create(ENSURE_INSTALLED_SDK_PERMISSIONS_TASK_NAME, task -> {
            task.doFirst(t -> {
                if (!ensureFileIsExecutable(emulatorConfiguration.getCmdLineToolsSdkManager())) {
                    throw new RuntimeException("Unable to make new SDK Manager executable");
                }

                if (!ensureFileIsExecutable(emulatorConfiguration.getAvdManager())) {
                    throw new RuntimeException("Unable to make Android Virtual Device manager executable");
                }
            });

            task.dependsOn(INSTALL_SDK_DEPENDENCIES_TASK_NAME);
        });
    }

    private static void createAddAdditionalSdkRepositoriesTask(final Project project) {
        project.getTasks().create(ADD_ADDITIONAL_SDK_REPOSITORIES_TASK_NAME, task -> {
            task.doFirst(t -> {
                try {
                    final AndroidRepositories repositories = AndroidRepositories.load();
                    repositories.addRepository("Legacy Google APIs System Images", new URL("https://dl.google.com/android/repository/sys-img/google_apis/sys-img.xml"));
                    repositories.addRepository("Legacy Android System Images", new URL("https://dl.google.com/android/repository/sys-img/android/sys-img.xml"));
                    repositories.save();
                } catch (AndroidRepositoryException | MalformedURLException e) {
                    throw new TaskExecutionException(t, e);
                }
            });
        });
    }

    private static void createInstallSdkDependenciesTask(final Project project, final EmulatorConfiguration emulatorConfiguration) {
        createExecTask(project, emulatorConfiguration, INSTALL_SDK_DEPENDENCIES_TASK_NAME, exec -> {
            exec.setExecutable(emulatorConfiguration.getSdkManager());
            exec.setArgs(l(getSdkRootArgument(emulatorConfiguration), "emulator", "cmdline-tools;latest"));
            exec.setStandardInput(buildStandardInLines("y"));
            exec.getOutputs().dir(new File(emulatorConfiguration.getSdkRoot(), "emulator"));

            exec.dependsOn(ENSURE_BASE_SDK_PERMISSIONS_TASK_NAME);

            // This cannot be a lambda or the task will never be considered up-to-date
            exec.doLast(new Action<Task>() {
                @Override
                public void execute(final Task task) {
                    if (!emulatorConfiguration.getEmulator().setExecutable(true)) {
                        throw new RuntimeException("Unable to make android emulator executable");
                    }
                }
            });
        });
    }

    private static void createInstallEmulatorSystemImageTask(final Project project, final EmulatorConfiguration emulatorConfiguration) {
        createExecTask(project, emulatorConfiguration, INSTALL_ANDROID_EMULATOR_SYSTEM_IMAGE_TASK_NAME, exec -> {
            exec.setExecutable(emulatorConfiguration.getCmdLineToolsSdkManager());
            exec.setArgs(l(getSdkRootArgument(emulatorConfiguration), emulatorConfiguration.getSystemImagePackageName()));
            exec.setStandardInput(buildStandardInLines("y"));
            exec.getOutputs().dir(emulatorConfiguration.sdkFile("system-images", emulatorConfiguration.getAndroidVersion(), emulatorConfiguration.getFlavor(), emulatorConfiguration.getAbi()));
            exec.getOutputs().file(emulatorConfiguration.sdkFile("system-images", emulatorConfiguration.getAndroidVersion(), emulatorConfiguration.getFlavor(), emulatorConfiguration.getAbi(), "system.img"));
            exec.dependsOn(ENSURE_INSTALLED_SDK_PERMISSIONS_TASK_NAME, ADD_ADDITIONAL_SDK_REPOSITORIES_TASK_NAME);
        });
    }

    private static void createCreateEmulatorTask(final Project project, final EmulatorConfiguration emulatorConfiguration) {
        final File avdRoot = emulatorConfiguration.getAvdRoot();
        final String emulatorName = emulatorConfiguration.getEmulatorName();
        final String emulatorDevice = emulatorConfiguration.getDeviceType();
        createExecTask(project, emulatorConfiguration, CREATE_ANDROID_EMULATOR_TASK_NAME, exec -> {
            exec.setExecutable(emulatorConfiguration.getAvdManager());
            List<String> args = l("create", "avd", "--name", emulatorName, "--package", emulatorConfiguration.getSystemImagePackageName(), "--force");
            if (emulatorDevice != null) {
                args.add("--device");
                args.add(emulatorDevice);
            }
            exec.setArgs(args);
            exec.setStandardInput(buildStandardInLines("no"));
            exec.getOutputs().dir(new File(avdRoot, emulatorName + ".avd"));
            exec.getOutputs().file(new File(avdRoot, emulatorName + ".ini"));

            exec.dependsOn(
                    INSTALL_ANDROID_EMULATOR_SYSTEM_IMAGE_TASK_NAME,
                    INSTALL_SDK_DEPENDENCIES_TASK_NAME,
                    ENSURE_INSTALLED_SDK_PERMISSIONS_TASK_NAME);
        });
    }

    private static void createEmulatorLifecycleTasks(final Project project, final EmulatorConfiguration emulatorConfiguration, final AdbProxy adbProxy) {
        final AtomicReference<Process> emulatorProcess = new AtomicReference<>();
        final AtomicReference<Process> waitForDeviceProcess = new AtomicReference<>();

        createStartEmulatorTask(project, emulatorConfiguration, emulatorProcess, waitForDeviceProcess);
        createWaitForEmulatorTask(project, emulatorConfiguration, adbProxy, waitForDeviceProcess);
        createStopEmulatorTask(project, emulatorProcess);
    }

    /**
     * Logs emulator output via new threads.
     *
     * @param process The process to log the output of
     * @param logger  The logger to report output with
     */
    private static void logOutput(final Process process, final Logger logger) {
        final InputStream stdout = process.getInputStream(); // NOPMD - These can't be closed outside of the thread
        final InputStream stderr = process.getErrorStream(); // NOPMD - These can't be closed outside of the thread
        new Thread(() -> {
            try (final InputStream stream = stdout) {
                IOUtils.lineIterator(stream, StandardCharsets.UTF_8).forEachRemaining(s -> logger.info("[Android Emulator - STDOUT] " + s));
            } catch (IOException | IllegalStateException e) {
                logger.error("Error reading Android emulator stdout", e);
            }
        }).start();
        new Thread(() -> {
            try (final InputStream stream = stderr) {
                IOUtils.lineIterator(stream, StandardCharsets.UTF_8).forEachRemaining(s -> logger.info("[Android Emulator - STDERR] " + s));
            } catch (IOException | IllegalStateException e) {
                logger.error("Error reading Android emulator stderr", e);
            }
        }).start();
    }

    private static void createStartEmulatorTask(final Project project, final EmulatorConfiguration emulatorConfiguration, final AtomicReference<Process> emulatorProcess, final AtomicReference<Process> waitForDeviceProcess) {
        final boolean logEmulatorOutput = emulatorConfiguration.getLogEmulatorOutput();

        final List<String> command = new ArrayList<>();
        command.add(emulatorConfiguration.getEmulator().getAbsolutePath());
        command.add("@" + emulatorConfiguration.getEmulatorName());

        // Allows the plugin to monitor the logs from the emulator and start the emulator synchronously. Without this,
        // the emulator would be detached from the process being build and be much more difficult to shut down.
        command.add("-shell");

        // Forces the emulator to ignore the stored snapshot and cold start up. This is currently used to ensure the
        // emulator has a predictable UUID associated with it.
        if (emulatorConfiguration.shouldForceColdStart()) {
            command.add("-no-snapshot-load");
        }

        // This sets a property retrievable via ADB that allows the plugin to identify the emulator launched. Without
        // it, the emulator serial could be guessed but not with 100% certainty. Emulator serials are currently of the
        // form "emulator-{port}" which may vary, depending on how many other emulators were launched.
        // Based on guidance from https://stackoverflow.com/a/42038655/1554990
        command.addAll(l("-prop", "emu.uuid=" + emulatorConfiguration.getEmulatorUuid()));

        command.addAll(emulatorConfiguration.getAdditionalEmulatorArguments());
        final ProcessBuilder pb = new ProcessBuilder(command.toArray(new String[0]));
        pb.environment().putAll(emulatorConfiguration.getEnvironmentVariableMap());
        if (!logEmulatorOutput) {
            pb.inheritIO();
        }

        project.getTasks().create(START_ANDROID_EMULATOR_TASK_NAME, task -> {
            task.doFirst(t -> {
                final Logger logger = project.getLogger();
                logger.debug("Starting emulator with command {} {}", pb.environment(), pb.command());
                try {
                    final Process directProcess = pb.start();
                    emulatorProcess.set(directProcess);
                    if (logEmulatorOutput) {
                        logOutput(directProcess, logger);
                    }
                    new Thread(() -> {
                        final int returnCode;
                        try {
                            returnCode = directProcess.waitFor();
                        } catch (InterruptedException e) {
                            logger.warn("Interrupted while watching emulator process", e);
                            // Do nothing
                            return;
                        }

                        if (returnCode != 0) {
                            final Process p = waitForDeviceProcess.get();
                            if (p != null) {
                                p.destroy();
                            }
                            logger.error("Emulator exited abnormally with return code " + returnCode);
                        }
                    }).start();
                    Runtime.getRuntime().addShutdownHook(new Thread(() -> emulatorProcess.getAndUpdate(DESTROY_AND_REPLACE_WITH_NULL)));
                } catch (final IOException e) {
                    throw new RuntimeException("Emulator failed to start successfully", e);
                }
            });

            task.dependsOn(ENSURE_INSTALLED_SDK_PERMISSIONS_TASK_NAME, INSTALL_SDK_DEPENDENCIES_TASK_NAME, CREATE_ANDROID_EMULATOR_TASK_NAME);
            task.finalizedBy(STOP_ANDROID_EMULATOR_TASK_NAME);
        });
    }

    /**
     * Uses ADB to query for connected android emulators
     * @param adbProxy Used to query ADB
     * @return A list of all connected emulator serials, e.g. {@code emulator-5554}.
     */
    private static List<String> getConnectedEmulatorSerials(final AdbProxy adbProxy) {
        final String[] stdout = adbProxy.execute("devices");
        List<String> emulators = new ArrayList<>();
        for (final String line : stdout) {
            final Matcher matcher = ADB_OUTPUT_PATTERN.matcher(line);
            if (matcher.matches() && matcher.groupCount() == 2) {
                // These emulator serial formats may change in the future and may lead to breakages
                if (matcher.group(1).startsWith("emulator-") && matcher.group(2).equals("device")) {
                    emulators.add(matcher.group(1));
                }
            }
        }
        return emulators;
    }

    private static void createWaitForEmulatorTask(final Project project, final EmulatorConfiguration emulatorConfiguration, final AdbProxy adbProxy, final AtomicReference<Process> waitForDeviceProcess) {
        project.getTasks().create(WAIT_FOR_ANDROID_EMULATOR_TASK_NAME, task -> {
            task.doFirst(t -> {
                String serial = null;
                // Attempt to find the emulator with the UUID. The shell will become available very early on in the boot
                // process, even if though the device isn't suitable for running tests quite yet. There's a chance the
                // emulator won't be visible to ADB right away, hence the need for a short loop with some sleeps.
                for (int i = 0; i < 60 && serial == null; i++) {
                    for (final String emulator : getConnectedEmulatorSerials(adbProxy)) {
                        final String[] out = adbProxy.execute("-s", emulator, "wait-for-device", "shell", "getprop", "emu.uuid");
                        if (out.length == 1 && out[0].equals(emulatorConfiguration.getEmulatorUuid())) {
                            serial = emulator;
                            break;
                        }
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        // Allow this to miss to give time
                    }
                }
                if (serial == null) {
                    throw new GradleException("Unable to detect plugin-managed emulator.");
                }

                // The AdbProxy cannot be used here as the process needs to run asynchronously in order for it to be
                // terminable if the Gradle run is aborted early.
                final List<String> command = new ArrayList<>();
                command.add(emulatorConfiguration.getAdb().getAbsolutePath());
                command.addAll(l("-s", serial, "wait-for-device", "shell", "while $(exit $(getprop sys.boot_completed)) ; do sleep 1; done;"));
                final ProcessBuilder pb = new ProcessBuilder(command.toArray(new String[0]));
                pb.environment().putAll(emulatorConfiguration.getEnvironmentVariableMap());
                try {
                    final Process p = pb.start();
                    waitForDeviceProcess.set(p);
                    p.waitFor();
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException("Unable to wait for emulator", e);
                }
            });

            task.getInputs().property("environmentMaps", emulatorConfiguration.getEnvironmentVariableMap());
            task.dependsOn(ENSURE_BASE_SDK_PERMISSIONS_TASK_NAME, START_ANDROID_EMULATOR_TASK_NAME);
        });
    }

    private static void createStopEmulatorTask(final Project project, final AtomicReference<Process> emulatorProcess) {
        project.getTasks().create(STOP_ANDROID_EMULATOR_TASK_NAME, task -> {
            task.doFirst(t -> emulatorProcess.getAndUpdate(DESTROY_AND_REPLACE_WITH_NULL));

            task.dependsOn(ENSURE_INSTALLED_SDK_PERMISSIONS_TASK_NAME, START_ANDROID_EMULATOR_TASK_NAME);
            task.mustRunAfter(WAIT_FOR_ANDROID_EMULATOR_TASK_NAME);
        });
    }

    /**
     * Generates the argument for specifying {@code --sdk_root} for the new {@code sdkmanager} which requires it.
     */
    private static String getSdkRootArgument(final EmulatorConfiguration emulatorConfiguration) {
        return "--sdk_root=" + emulatorConfiguration.getSdkRoot().getAbsolutePath();
    }

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
    @SuppressWarnings("varargs")
    private static <T> ArrayList<T> l(T... arr) {
        return new ArrayList<>(Arrays.asList(arr));
    }

    private static InputStream buildStandardInLines(String... lines) {
        final String string = String.join(System.lineSeparator(), lines);
        final byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
        return new ByteArrayInputStream(bytes);
    }

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
            final AdbProxy adbProxy = new AdbProxy(project, emulatorConfiguration);

            if (emulatorConfiguration.getEnableForAndroidTests()) {
                setUpAndroidTests(p);
            }


            createEnsurePermissionsTasks(p, emulatorConfiguration);
            createAddAdditionalSdkRepositoriesTask(p);
            createInstallSdkDependenciesTask(p, emulatorConfiguration);
            createInstallEmulatorSystemImageTask(p, emulatorConfiguration);
            createCreateEmulatorTask(p, emulatorConfiguration);
            createEmulatorLifecycleTasks(p, emulatorConfiguration, adbProxy);
        });
    }
}