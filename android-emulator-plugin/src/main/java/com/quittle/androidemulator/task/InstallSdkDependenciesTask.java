package com.quittle.androidemulator.task;

import com.quittle.androidemulator.EmulatorConfiguration;
import org.gradle.api.Action;
import org.gradle.api.Task;

import javax.inject.Inject;
import java.io.File;
import java.util.Arrays;

public class InstallSdkDependenciesTask extends AndroidEmulatorBaseExecTask<InstallSdkDependenciesTask> {
    @Inject
    public InstallSdkDependenciesTask(final EmulatorConfiguration emulatorConfiguration) {
        super(InstallSdkDependenciesTask.class, emulatorConfiguration);

        this.setExecutable(emulatorConfiguration.getSdkManager());
        this.args(Arrays.asList(buildSdkRootArgument(), "emulator", "cmdline-tools;latest", "platform-tools"));
        this.args(emulatorConfiguration.getAdditionalSdkManagerArguments());
        this.setStandardInput(buildStandardInLines("y"));
        this.getOutputs().dir(new File(emulatorConfiguration.getSdkRoot(), "emulator"));

        // This cannot be a lambda or the task will never be considered up-to-date
        this.doLast(new FixPermissions(emulatorConfiguration));
    }

    private static class FixPermissions implements Action<Task> {
        private final EmulatorConfiguration emulatorConfiguration;

        private FixPermissions(final EmulatorConfiguration emulatorConfiguration) {
            this.emulatorConfiguration = emulatorConfiguration;
        }

        @Override
        public void execute(Task task) {
            if (!emulatorConfiguration.getEmulator().setExecutable(true)) {
                throw new RuntimeException("Unable to make android emulator executable");
            }
        }
    }
}
