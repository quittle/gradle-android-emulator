package com.quittle.androidemulator.task;

import org.gradle.api.Project;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

/**
 * Utility that attempts to destroy a process. This operator always returns null, which is useful for nulling out an
 * {@link java.util.concurrent.atomic.AtomicReference} with {@code processReference.getAndUpdate(processDestroyer)}.
 */
class ProcessDestroyer implements UnaryOperator<Process> {
    private static final long PROCESS_TERMINATION_TIMEOUT_SEC = 15;

    private final Logger logger;

    ProcessDestroyer(final Project project) {
        this.logger = project.getLogger();
    }

    @Override
    public Process apply(final Process process) {
        if (process != null) {
			List<ProcessHandle> descendants = process.descendants().collect(Collectors.toList());
            // Use a non-forceful destroy first to allow the process to gracefully shutdown. With the android emulator
            // this includes creating a snapshot of the current state for warm boots in subsequent runs. On unix-like
            // systems this usually translates to raising a SIGTERM signal.
            process.destroy();
            try {
                final boolean processDidExit = process.waitFor(PROCESS_TERMINATION_TIMEOUT_SEC, TimeUnit.SECONDS);

                // Forcibly destroy the process. On unix-like systems this usually translates to raising a SIGKILL
                // signal.
                if (!processDidExit) {
                    process.destroyForcibly();
                    process.waitFor(PROCESS_TERMINATION_TIMEOUT_SEC, TimeUnit.SECONDS);
                }
				// if a process has not destroyed descendants
				descendants.forEach(ProcessHandle::destroyForcibly);
            } catch (InterruptedException e) {
                logger.debug("Interrupted while waiting for process to be destroyed", e);
                // It is likely fine to allow the failure and move on. The termination of the gradle process might stop
                // the process or stop soon after the timeout.
            }
        }

        return null;
    }
}
