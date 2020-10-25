package com.quittle.androidemulator.task;

import com.quittle.androidemulator.AdbProxy;
import com.quittle.androidemulator.EmulatorConfiguration;
import org.apache.commons.io.IOUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StartAndroidEmulatorTask extends DefaultTask {
    /**
     * This is matching adb output lines. These emulator serial formats may change in the future and may lead to
     * breakages. Example ADB Output
     * <pre>{@code
     * List of devices attached
     * emulator-5554       device
     * 192.168.1.2:42839   device
     * }</pre>
     */
    private static final Pattern ADB_OUTPUT_EMULATOR_PATTERN = Pattern.compile("(emulator-(\\d{1,5}))\\s+device");
    private static final UnaryOperator<Process> DESTROY_AND_REPLACE_WITH_NULL = process -> {
        if (process != null) {
            process.destroy();
            process.destroyForcibly();
        }

        return null;
    };

    private final EmulatorConfiguration emulatorConfiguration;
    private final AdbProxy adbProxy;
    private final AtomicReference<Process> emulatorProcess;
    private final AtomicReference<Process> waitForDeviceProcess;

    @Inject
    public StartAndroidEmulatorTask(
            final EmulatorConfiguration emulatorConfiguration,
            final AdbProxy adbProxy,
            final AtomicReference<Process> emulatorProcess,
            final AtomicReference<Process> waitForDeviceProcess) {
        this.emulatorConfiguration = emulatorConfiguration;
        this.adbProxy = adbProxy;
        this.emulatorProcess = emulatorProcess;
        this.waitForDeviceProcess = waitForDeviceProcess;
    }

    @TaskAction
    public void act() {
        final boolean logEmulatorOutput = emulatorConfiguration.getLogEmulatorOutput();

        final int proposedEmulatorPort = findAcceptableEmulatorPort(adbProxy);
        emulatorConfiguration.setEmulatorPort(proposedEmulatorPort);

        final List<String> command = new ArrayList<>();
        command.add(emulatorConfiguration.getEmulator().getAbsolutePath());
        command.add("@" + emulatorConfiguration.getEmulatorName());

        // Allows the plugin to monitor the logs from the emulator and start the emulator synchronously. Without this,
        // the emulator would be detached from the process being build and be much more difficult to shut down.
        command.add("-shell");

        command.addAll(Arrays.asList("-port", String.valueOf(proposedEmulatorPort)));

        command.addAll(emulatorConfiguration.getAdditionalEmulatorArguments());
        final ProcessBuilder pb = new ProcessBuilder(command.toArray(new String[0]));
        pb.environment().putAll(emulatorConfiguration.getEnvironmentVariableMap());
        if (!logEmulatorOutput) {
            pb.inheritIO();
        }

        final Logger logger = getLogger();
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

    private static int findAcceptableEmulatorPort(final AdbProxy adbProxy) {
        final Set<Integer> reservedPorts =
                Stream.of(adbProxy.execute("devices"))
                        .map(ADB_OUTPUT_EMULATOR_PATTERN::matcher)
                        .filter(Matcher::matches)
                        .map(matcher -> Integer.parseInt(matcher.group(2)))
                        .collect(Collectors.toSet());

        // Start at the top of the range and iterate down to increase the likelihood of getting an earlier match.
        for (int port = 5680; port >= 5554; port -= 2) {
            if (!reservedPorts.contains(port)) {
                return port;
            }
        }

        throw new GradleException("No viable emulator ports found");
    }
}
