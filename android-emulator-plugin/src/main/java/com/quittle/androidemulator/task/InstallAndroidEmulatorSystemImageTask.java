package com.quittle.androidemulator.task;

import com.quittle.androidemulator.EmulatorConfiguration;

import javax.inject.Inject;
import java.util.Arrays;

public class InstallAndroidEmulatorSystemImageTask extends AndroidEmulatorBaseExecTask<InstallAndroidEmulatorSystemImageTask> {
    @Inject
    public InstallAndroidEmulatorSystemImageTask(final EmulatorConfiguration emulatorConfiguration) {
        super(InstallAndroidEmulatorSystemImageTask.class, emulatorConfiguration);

        this.setExecutable(emulatorConfiguration.getCmdLineToolsSdkManager());
        this.args(Arrays.asList(buildSdkRootArgument(), emulatorConfiguration.getSystemImagePackageName()));
        this.args(emulatorConfiguration.getAdditionalSdkManagerArguments());
        this.setStandardInput(buildStandardInLines("y"));
        this.getOutputs().dir(emulatorConfiguration.sdkFile("system-images", emulatorConfiguration.getAndroidVersion(), emulatorConfiguration.getFlavor(), emulatorConfiguration.getAbi()));
        this.getOutputs().file(emulatorConfiguration.sdkFile("system-images", emulatorConfiguration.getAndroidVersion(), emulatorConfiguration.getFlavor(), emulatorConfiguration.getAbi(), "system.img"));
    }
}
