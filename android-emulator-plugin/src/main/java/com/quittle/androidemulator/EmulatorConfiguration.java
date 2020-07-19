package com.quittle.androidemulator;

import com.android.build.gradle.BaseExtension;
import com.android.builder.model.ApiVersion;
import com.google.common.collect.ImmutableMap;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.gradle.api.Project;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.List;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class EmulatorConfiguration {
    private final File sdkRoot;
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
    private final String deviceType;

    EmulatorConfiguration(final Project project, final BaseExtension androidExtension, final AndroidEmulatorExtension androidEmulatorExtension) {
        this.sdkRoot = androidExtension.getSdkDirectory();

        if (androidEmulatorExtension.getAvdRoot() != null) {
            this.avdRoot = androidEmulatorExtension.getAvdRoot();
        } else {
            this.avdRoot = new File(project.getBuildDir(), "android-avd-root");
        }

        if (this.sdkRoot == null) {
            throw new RuntimeException("Unable to initialize com.quittle.android-emulator " +
                    "because Android plugin has not been initialized with an SDK root.");
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
        this.deviceType = emulator.getDevice();

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
        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            return sdkFile(sdkRoot,"tools", "bin", "sdkmanager.bat");
        } else {
            return sdkFile(sdkRoot,"tools", "bin", "sdkmanager");
        }
    }

    File getCmdLineToolsSdkManager() {
        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            return sdkFile(sdkRoot,"cmdline-tools", "latest", "bin", "sdkmanager.bat");
        } else {
            return sdkFile(sdkRoot,"cmdline-tools", "latest", "bin", "sdkmanager");
        }
    }

    File getAvdManager() {
        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            return sdkFile(sdkRoot,"cmdline-tools", "latest", "bin", "avdmanager.bat");
        } else {
            return sdkFile(sdkRoot,"cmdline-tools", "latest", "bin", "avdmanager");
        }
    }

    File getEmulator() {
        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            return sdkFile(sdkRoot,"emulator", "emulator.exe");
        } else {
            return sdkFile(sdkRoot,"emulator", "emulator");
        }
    }

    File getAdb() {
        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            return sdkFile(sdkRoot,"platform-tools", "adb.exe");
        } else {
            return sdkFile(sdkRoot,"platform-tools", "adb");
        }
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

    String getDeviceType() {
        return deviceType;
    }

    private static File sdkFile(final File sdkRoot, final String... path) {
        return new File(sdkRoot, String.join(File.separator, path));
    }
}
