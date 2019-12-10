package com.quittle.androidemulator;

import com.android.build.gradle.BaseExtension;
import com.android.builder.model.ApiVersion;
import com.google.common.collect.ImmutableMap;
import org.gradle.api.Project;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.List;

class EmulatorConfiguration {
    private final File sdkRoot;
    private final File sdkManager;
    private final File avdManager;
    private final File emulator;
    private final File adb;
    private final File avdRoot;
    private final Map<String, String> environmentVariableMap;
    private final boolean enableForAndroidTests;
    private final List<String> additionalEmulatorArguments;
    private final boolean logEmulatorOutput;
    private final String androidVersion;
    private final String flavor;
    private final String abi;
    private final String systemImagePackageName;
    private final String emulatorName;

    EmulatorConfiguration(final Project project, final BaseExtension androidExtension, final AndroidEmulatorExtension androidEmulatorExtension) {
        this.sdkRoot = androidExtension.getSdkDirectory();
        this.sdkManager = sdkFile(sdkRoot,"tools", "bin", "sdkmanager");
        this.avdManager = sdkFile(sdkRoot,"tools", "bin", "avdmanager");
        this.emulator = sdkFile(sdkRoot,"emulator", "emulator");
        this.adb = sdkFile(sdkRoot,"platform-tools", "adb");

        if (androidEmulatorExtension.getAvdRoot() != null) {
            this.avdRoot = androidEmulatorExtension.getAvdRoot();
        } else {
            this.avdRoot = new File(project.getBuildDir(), "android-avd-root");
        }

        this.environmentVariableMap = ImmutableMap.of(
                "ANDROID_SDK_ROOT", sdkRoot.getAbsolutePath(),
                "ANDROID_HOME", sdkRoot.getAbsolutePath(),
                "ANDROID_AVD_HOME", avdRoot.getAbsolutePath());
        this.enableForAndroidTests = androidEmulatorExtension.getEnableForAndroidTests();

        this.additionalEmulatorArguments = new ArrayList<>();
        if (androidEmulatorExtension.getHeadless()) {
            additionalEmulatorArguments.add("-no-skin");
            additionalEmulatorArguments.add("-no-audio");
            additionalEmulatorArguments.add("-no-window");
        }
        final String[] additionalArgs = androidEmulatorExtension.getAdditionalEmulatorArguments();
        if (additionalArgs != null) {
            additionalEmulatorArguments.addAll(Arrays.asList(additionalArgs));
        }

        this.logEmulatorOutput = androidEmulatorExtension.getLogEmulatorOutput();

        final AndroidEmulatorExtension.EmulatorExtension emulator = androidEmulatorExtension.getEmulator();
        int sdkVersion = emulator.getSdkVersion();
        if (sdkVersion <= 0) {
            ApiVersion version = androidExtension.getDefaultConfig().getTargetSdkVersion();
            if (version == null) {
                version = androidExtension.getDefaultConfig().getMinSdkVersion();
            }

            if (version != null) {
                sdkVersion = version.getApiLevel();
            } else {
                sdkVersion = 10; // Earliest version supported out of the box by the SDK Manager still
            }
        }
        this.androidVersion = String.format("android-%d", sdkVersion);
        this.flavor = emulator.getIncludeGoogleApis() ? "google_apis" : "default";
        this.abi = emulator.getAbi();
        this.systemImagePackageName = String.format("system-images;%s;%s;%s", androidVersion, flavor, abi);

        if (emulator.getName() != null) {
            this.emulatorName = emulator.getName();
        } else {
            this.emulatorName = String.format("generated-%s_%s-%s", androidVersion, abi, flavor);
        }
    }

    File getSdkRoot() {
        return sdkRoot;
    }

    File sdkFile(String... path) {
        return sdkFile(sdkRoot, path);
    }

    File getSdkManager() {
        return sdkManager;
    }

    File getAvdManager() {
        return avdManager;
    }

    File getEmulator() {
        return emulator;
    }

    File getAdb() {
        return adb;
    }

    File getAvdRoot() {
        return avdRoot;
    }

    Map<String, String> getEnvironmentVariableMap() {
        return environmentVariableMap;
    }

    boolean getEnableForAndroidTests() {
        return enableForAndroidTests;
    }

    List<String> getAdditionalEmulatorArguments() {
        return additionalEmulatorArguments;
    }

    boolean getLogEmulatorOutput() {
        return logEmulatorOutput;
    }

    String getAndroidVersion() {
        return androidVersion;
    }

    String getFlavor() {
        return flavor;
    }

    String getAbi() {
        return abi;
    }

    String getSystemImagePackageName() {
        return systemImagePackageName;
    }

    String getEmulatorName() {
        return emulatorName;
    }

    private static File sdkFile(final File sdkRoot, final String... path) {
        return new File(sdkRoot, String.join(File.separator, path));
    }
}
