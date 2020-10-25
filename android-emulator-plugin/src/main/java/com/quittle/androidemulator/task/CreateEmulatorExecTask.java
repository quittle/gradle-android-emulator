package com.quittle.androidemulator.task;

import com.quittle.androidemulator.EmulatorConfiguration;

import javax.inject.Inject;
import java.io.File;
import java.util.List;

import static com.quittle.androidemulator.CollectionUtils.mutableListOf;

public class CreateEmulatorExecTask extends AndroidEmulatorBaseExecTask<CreateEmulatorExecTask> {
    @Inject
    public CreateEmulatorExecTask(final EmulatorConfiguration emulatorConfiguration) {
        super(CreateEmulatorExecTask.class, emulatorConfiguration);

        final String emulatorName = emulatorConfiguration.getEmulatorName();
        final File avdRoot = emulatorConfiguration.getAvdRoot();
        final String deviceType = emulatorConfiguration.getDeviceType();
        final String systemImagePackageName = emulatorConfiguration.getSystemImagePackageName();

        this.setExecutable(emulatorConfiguration.getAvdManager());
        List<String> args = mutableListOf(
                "create",
                "avd",
                "--name", emulatorName,
                "--package", systemImagePackageName,
                "--force");
        if (deviceType != null) {
            args.add("--device");
            args.add(deviceType);
        }
        this.setArgs(args);
        this.setStandardInput(buildStandardInLines("no"));

        this.getOutputs().dir(new File(avdRoot, emulatorName + ".avd"));
        this.getOutputs().file(new File(avdRoot, emulatorName + ".ini"));
    }
}
