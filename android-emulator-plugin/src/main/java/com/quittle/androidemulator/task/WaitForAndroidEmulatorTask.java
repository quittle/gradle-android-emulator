package com.quittle.androidemulator.task;

import com.quittle.androidemulator.EmulatorConfiguration;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class WaitForAndroidEmulatorTask extends DefaultTask {
    private final EmulatorConfiguration emulatorConfiguration;
    private final AtomicReference<Process> waitForDeviceProcess;

    @Inject
    public WaitForAndroidEmulatorTask(final EmulatorConfiguration emulatorConfiguration, final AtomicReference<Process> waitForDeviceProcess) {
        this.emulatorConfiguration = emulatorConfiguration;
        this.waitForDeviceProcess = waitForDeviceProcess;
    }

    @TaskAction
    public void act() {
        // The AdbProxy cannot be used here as the process needs to run asynchronously in order for it to be
        // terminable if the Gradle run is aborted early.
        final List<String> command = new ArrayList<>();
        command.add(emulatorConfiguration.getAdb().getAbsolutePath());
        command.addAll(Arrays.asList(
                "-s", "emulator-" + emulatorConfiguration.getEmulatorPort(),
                "wait-for-device",
                "shell",
                "while $(exit $(getprop sys.boot_completed)) ; do sleep 1; done;"));
        final ProcessBuilder pb = new ProcessBuilder(command.toArray(new String[0]));
        pb.environment().putAll(emulatorConfiguration.getEnvironmentVariableMap());
        try {
            final Process p = pb.start();
            waitForDeviceProcess.set(p);
            p.waitFor();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Unable to wait for emulator", e);
        }
    }
}
