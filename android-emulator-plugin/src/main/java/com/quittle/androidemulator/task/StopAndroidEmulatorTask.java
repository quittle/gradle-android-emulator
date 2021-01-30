package com.quittle.androidemulator.task;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

public class StopAndroidEmulatorTask extends DefaultTask {
    final AtomicReference<Process> emulatorProcess;

    @Inject
    public StopAndroidEmulatorTask(final AtomicReference<Process> emulatorProcess) {
        this.emulatorProcess = emulatorProcess;
    }

    @TaskAction
    public void act() {
        emulatorProcess.getAndUpdate(new ProcessDestroyer(getProject()));
    }
}
