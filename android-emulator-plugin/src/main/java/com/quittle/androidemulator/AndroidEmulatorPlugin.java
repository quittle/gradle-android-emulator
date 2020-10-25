package com.quittle.androidemulator;

import com.android.build.gradle.BaseExtension;
import com.android.build.gradle.internal.tasks.DeviceProviderInstrumentTestTask;
import com.quittle.androidemulator.task.*;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.TaskInstantiationException;

import java.util.concurrent.atomic.AtomicReference;

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

    private static void setUpAndroidTests(final Project project) {
        project.getTasks().withType(
                DeviceProviderInstrumentTestTask.class, task -> {
                    task.dependsOn(WAIT_FOR_ANDROID_EMULATOR_TASK_NAME);
                    task.finalizedBy(STOP_ANDROID_EMULATOR_TASK_NAME);
                });
    }

    private static void createEnsurePermissionsTasks(final Project project, final EmulatorConfiguration emulatorConfiguration) {
        project.getTasks().create(ENSURE_BASE_SDK_PERMISSIONS_TASK_NAME, EnsureBaseSdkPermissions.class, emulatorConfiguration);

        final Task ensureInstalledSdkPermissionsTask = project.getTasks().create(ENSURE_INSTALLED_SDK_PERMISSIONS_TASK_NAME, EnsureInstalledSdkPermissionsTask.class, emulatorConfiguration);
        ensureInstalledSdkPermissionsTask.dependsOn(INSTALL_SDK_DEPENDENCIES_TASK_NAME);
    }

    private static void createAddAdditionalSdkRepositoriesTask(final Project project) {
        project.getTasks().create(ADD_ADDITIONAL_SDK_REPOSITORIES_TASK_NAME, AddAdditionalSdkRepositoriesTask.class);
    }

    private static void createInstallSdkDependenciesTask(final Project project, final EmulatorConfiguration emulatorConfiguration) {
        final Task task = project.getTasks().create(INSTALL_SDK_DEPENDENCIES_TASK_NAME, InstallSdkDependenciesTask.class, emulatorConfiguration);
        task.dependsOn(ENSURE_BASE_SDK_PERMISSIONS_TASK_NAME);
    }

    private static void createInstallEmulatorSystemImageTask(final Project project, final EmulatorConfiguration emulatorConfiguration) {
        final Task task = project.getTasks().create(INSTALL_ANDROID_EMULATOR_SYSTEM_IMAGE_TASK_NAME, InstallAndroidEmulatorSystemImageTask.class, emulatorConfiguration);
        task.dependsOn(ENSURE_INSTALLED_SDK_PERMISSIONS_TASK_NAME, ADD_ADDITIONAL_SDK_REPOSITORIES_TASK_NAME);
    }

    private static void createCreateEmulatorTask(final Project project, final EmulatorConfiguration emulatorConfiguration) {
        final Task createEmulatorTask = project.getTasks().create(
                CREATE_ANDROID_EMULATOR_TASK_NAME, CreateEmulatorExecTask.class, emulatorConfiguration);
        createEmulatorTask.dependsOn(
                INSTALL_ANDROID_EMULATOR_SYSTEM_IMAGE_TASK_NAME,
                INSTALL_SDK_DEPENDENCIES_TASK_NAME,
                ENSURE_INSTALLED_SDK_PERMISSIONS_TASK_NAME);
    }

    private static void createEmulatorLifecycleTasks(final Project project, final EmulatorConfiguration emulatorConfiguration, final AdbProxy adbProxy) {
        final AtomicReference<Process> emulatorProcess = new AtomicReference<>();
        final AtomicReference<Process> waitForDeviceProcess = new AtomicReference<>();

        createStartEmulatorTask(project, emulatorConfiguration, adbProxy, emulatorProcess, waitForDeviceProcess);
        createWaitForEmulatorTask(project, emulatorConfiguration, waitForDeviceProcess);
        createStopEmulatorTask(project, emulatorProcess);
    }

    private static void createStartEmulatorTask(
            final Project project,
            final EmulatorConfiguration emulatorConfiguration,
            final AdbProxy adbProxy,
            final AtomicReference<Process> emulatorProcess,
            final AtomicReference<Process> waitForDeviceProcess) {
        final Task task = project.getTasks().create(START_ANDROID_EMULATOR_TASK_NAME, StartAndroidEmulatorTask.class,
                emulatorConfiguration, adbProxy, emulatorProcess, waitForDeviceProcess);

        task.dependsOn(ENSURE_INSTALLED_SDK_PERMISSIONS_TASK_NAME, INSTALL_SDK_DEPENDENCIES_TASK_NAME, CREATE_ANDROID_EMULATOR_TASK_NAME);
        task.finalizedBy(STOP_ANDROID_EMULATOR_TASK_NAME);
    }

    private static void createWaitForEmulatorTask(final Project project, final EmulatorConfiguration emulatorConfiguration, final AtomicReference<Process> waitForDeviceProcess) {
        final Task task = project.getTasks().create(WAIT_FOR_ANDROID_EMULATOR_TASK_NAME, WaitForAndroidEmulatorTask.class, emulatorConfiguration, waitForDeviceProcess);

        task.dependsOn(ENSURE_BASE_SDK_PERMISSIONS_TASK_NAME, START_ANDROID_EMULATOR_TASK_NAME);
    }

    private static void createStopEmulatorTask(final Project project, final AtomicReference<Process> emulatorProcess) {
        final Task task = project.getTasks().create(STOP_ANDROID_EMULATOR_TASK_NAME, StopAndroidEmulatorTask.class, emulatorProcess);

        task.dependsOn(ENSURE_INSTALLED_SDK_PERMISSIONS_TASK_NAME, START_ANDROID_EMULATOR_TASK_NAME);
        task.mustRunAfter(WAIT_FOR_ANDROID_EMULATOR_TASK_NAME);
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