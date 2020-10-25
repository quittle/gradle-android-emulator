package com.quittle.androidemulator.task;

import com.quittle.androidemulator.EmulatorConfiguration;
import org.gradle.api.tasks.AbstractExecTask;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class AndroidEmulatorBaseExecTask<T extends AndroidEmulatorBaseExecTask<T>> extends AbstractExecTask<T> {
    private final EmulatorConfiguration emulatorConfiguration;

    public AndroidEmulatorBaseExecTask(final Class<T> taskType, final EmulatorConfiguration emulatorConfiguration) {
        super(taskType);

        this.emulatorConfiguration = emulatorConfiguration;

        this.environment(emulatorConfiguration.getEnvironmentVariableMap());
        // Environment isn't tracked by default to prevent unrelated environment changes changing inpts
        this.getInputs().property("environmentMap", emulatorConfiguration.getEnvironmentVariableMap());
    }

    /**
     * Generates the argument for specifying {@code --sdk_root} for the new {@code sdkmanager} which requires it.
     * @return The sdkmanager argument specifying the sdk root
     */
    protected String buildSdkRootArgument() {
        return "--sdk_root=" + emulatorConfiguration.getSdkRoot().getAbsolutePath();
    }

    /**
     * Provides an input stream from the lines of input. This is intended to be used to generate the standard input for
     * exec tasks.
     * @param lines The input lines
     * @return The generated input stream.
     */
    protected InputStream buildStandardInLines(String... lines) {
        final String string = String.join(System.lineSeparator(), lines);
        final byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
        return new ByteArrayInputStream(bytes);
    }
}
