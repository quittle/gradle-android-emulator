package com.quittle.androidemulator.task;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

public class StopAndroidEmulatorTask extends DefaultTask {
    private static final UnaryOperator<Process> DESTROY_AND_REPLACE_WITH_NULL = process -> {
        if (process != null) {
            process.destroy();
            process.destroyForcibly();
        }

        return null;
    };

    final AtomicReference<Process> emulatorProcess;

    @Inject
    public StopAndroidEmulatorTask(final AtomicReference<Process> emulatorProcess) {
        this.emulatorProcess = emulatorProcess;
    }

    @TaskAction
    public void act() {
        emulatorProcess.getAndUpdate(DESTROY_AND_REPLACE_WITH_NULL);
    }
}
