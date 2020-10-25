package com.quittle.androidemulator.task;

import com.quittle.androidemulator.EmulatorConfiguration;

import javax.inject.Inject;
import java.io.File;

public class EnsureInstalledSdkPermissionsTask extends EnsureFilesAreExecutableTask {
    private final EmulatorConfiguration emulatorConfiguration;

    @Inject
    public EnsureInstalledSdkPermissionsTask(final EmulatorConfiguration emulatorConfiguration) {
        this.emulatorConfiguration = emulatorConfiguration;
    }

    @Override
    protected File[] getFilesToMakeExecutable() {
        return new File[] {
                emulatorConfiguration.getCmdLineToolsSdkManager(),
                emulatorConfiguration.getAvdManager()
        };
    }
}
