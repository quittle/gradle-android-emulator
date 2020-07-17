# Android Emulator Plugin [![Gradle Plugin Version](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/com/quittle/android-emulator-plugin/maven-metadata.xml.svg?label=Gradle+Plugin+Version)](https://plugins.gradle.org/plugin/com.quittle.android-emulator) [![Build Status](https://travis-ci.com/quittle/gradle-android-emulator.svg?branch=master)](https://travis-ci.com/quittle/gradle-android-emulator)

This plugin adds tasks that install an Android emulator and start it up when running instrumentation tests
installs the Android Emulator and starts it up when running the instrumentation tests. Includes configuration
for specifying the emulator configuration.

**This plugin will automatically accept all Android SDK licenses when installing the emulator and emulator
system images. Before using the plugin or upgrading Android SDK versions, make sure you are okay accepting the
licenses for those versions.**

## Consumption

The minimum requirement for consumption is to simply
[apply this plugin](https://plugins.gradle.org/plugin/com.quittle.android-emulator).

#### build.gradle
```groovy
// Consume from Gradle plugin respository. This is the only required step.
plugins {
    id 'com.quittle.android-emulator' version '0.0.1'
}

// Consume android plugin as usual.
apply plugin: 'android'

android {
    // Fill out normally
}

// Optional configuration
androidEmulator {
    emulator {
        name 'my_avd_emulator_name' // Defaults to be dynamically based on the configuration of the AVD
        device 'pixel_xl' // Defaults to exclude the device flag, using avdmanager default. For options, run avdmanager list device
        sdkVersion 28 // Defaults to (Target SDK), then (Min SDK), then finally 10
        abi 'x86_64' // Defaults to x86
        includeGoogleApis true // Defaults to false
    }

    enableForAndroidTests false // Defaults to true
    avdRoot '~/.android/avd' // Defaults to be <gradle-build-dir>/android-avd-root
    headless true // Defaults to false but should be set to true for most CI systems
    additionalEmulatorArguments '-no-snapshot', '-http-proxy=localhost:1234' // Additional arguments to pass to the emulator at startup. See https://developer.android.com/studio/run/emulator-commandline#startup-options for options
    logEmulatorOutput true // Defaults to false but can be enabled to have emulator output logged for debugging.
}
```

## Development

In general, perform builds in the context of each folder, rather than as a multi-project Gradle
build. This is necessary because the `example-android-project` will fail to configure without the
plugin being locally available so the `android-emulator-plugin` project must be built and deployed
locally first.

In general, to build and test locally.
```
$ ./gradlew -p android-emulator-plugin # This runs all the default tasks
$ ./gradlew -p android-emulator-plugin publishToMavenLocal # Publishes it for the example-android-project to consume
$ ./validate_plugin # Integration test to validate the plugin works
```

## Deployment
This package is deployed via [Travis CI](https://travis-ci.com/quittle/gradle-android-emulator).
See `.travis.yml` for the CI/CD setup.

In the configuration for the build on Travis, `GRADLE_PUBLISH_KEY` and `GRADLE_PUBLISH_SECRET` are
injected as secret environment variables.

Upon check-in to the `master` branch, Travis checks out, builds, and deploys the plugin.
