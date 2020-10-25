package com.quittle.androidemulator.task;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import java.io.File;

/**
 * Ensures given files are executable, and if not, tries promote file to be executable, failing if unable to do so.
 */
public abstract class EnsureFilesAreExecutableTask extends DefaultTask {
    @TaskAction
    public void act() {
        for (final File file : getFilesToMakeExecutable()) {
            // First check if file is executable. In most environments sdk binaries are already executable.
            // In some environments your process is not an owner of the file and can't change permission.
            // For example docker or some CI systems in which case these files must already be executable.
            if (!(file.canExecute() || file.setExecutable(true))) {
                throw new RuntimeException(String.format("Unable to ensure %s is executable", file.getName()));
            }
        }
    }

    abstract protected File[] getFilesToMakeExecutable();
}
