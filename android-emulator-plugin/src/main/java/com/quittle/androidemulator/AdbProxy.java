package com.quittle.androidemulator;

import org.apache.commons.io.output.NullOutputStream;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.process.ExecResult;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Provides a simplified abstraction of running ADB commands.
 */
public class AdbProxy {
    private final Project project;
    private final EmulatorConfiguration emulatorConfiguration;

    public AdbProxy(final Project project, final EmulatorConfiguration emulatorConfiguration) {
        this.project = project;
        this.emulatorConfiguration = emulatorConfiguration;
    }

    /**
     * Invokes {@code ADB} with the provided arguments, returning it's output.
     * @param arguments The arguments to pass to ADB.
     * @return The lines of standard output emitted by ADB. The standard error is discarded.
     * @throws GradleException if the ADB command exits with a non-zero exit code.
     */
    public String[] execute(String... arguments) throws GradleException {
        final ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        final ExecResult result = project.exec(execSpec -> {
            execSpec.setExecutable(emulatorConfiguration.getAdb());
            execSpec.setArgs(Arrays.asList(arguments));
            execSpec.setEnvironment(emulatorConfiguration.getEnvironmentVariableMap());

            // Capture the stdout and throw away the stderr. Default is to forward to the process's stdout/stderr
            execSpec.setStandardOutput(stdout);
            execSpec.setErrorOutput(NullOutputStream.NULL_OUTPUT_STREAM);
        });

        // Assert it ran successfully
        result.assertNormalExitValue();
        result.rethrowFailure();

        // Save the stdout to a string, split on Unix newlines, and trip whitespace which potentially includes carriage
        // returns on Windows.
        String stdoutString = new String(stdout.toByteArray(), StandardCharsets.UTF_8);
        project.getLogger().debug("ADB stdout: " + stdoutString);
        String[] lines = stdoutString.split("\\n");
        for (int i = 0; i < lines.length; i++) {
            lines[i] = lines[i].trim();
        }
        return lines;
    }
}
