package com.quittle.androidemulator.task;

import com.quittle.androidemulator.AndroidRepositories;
import com.quittle.androidemulator.AndroidRepositoryException;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;

import java.net.MalformedURLException;
import java.net.URL;

public class AddAdditionalSdkRepositoriesTask extends DefaultTask {
    @TaskAction
    public void act() {
        try {
            final AndroidRepositories repositories = AndroidRepositories.load();
            repositories.addRepository("Legacy Google APIs System Images", new URL("https://dl.google.com/android/repository/sys-img/google_apis/sys-img.xml"));
            repositories.addRepository("Legacy Android System Images", new URL("https://dl.google.com/android/repository/sys-img/android/sys-img.xml"));
            repositories.save();
        } catch (AndroidRepositoryException | MalformedURLException e) {
            throw new TaskExecutionException(this, e);
        }
    }
}
