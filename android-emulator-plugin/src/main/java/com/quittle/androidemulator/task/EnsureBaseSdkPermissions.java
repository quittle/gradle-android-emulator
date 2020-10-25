package com.quittle.androidemulator.task;

import com.quittle.androidemulator.EmulatorConfiguration;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;
import java.io.File;

public class EnsureBaseSdkPermissions extends EnsureFilesAreExecutableTask {
    private final EmulatorConfiguration emulatorConfiguration;

    @Inject
    public EnsureBaseSdkPermissions(EmulatorConfiguration emulatorConfiguration) {
        this.emulatorConfiguration = emulatorConfiguration;
    }

    @InputFiles
    @Override
    protected File[] getFilesToMakeExecutable() {
        return new File[] {
                emulatorConfiguration.getSdkManager(),
                emulatorConfiguration.getAdb()
        };
    }
}
