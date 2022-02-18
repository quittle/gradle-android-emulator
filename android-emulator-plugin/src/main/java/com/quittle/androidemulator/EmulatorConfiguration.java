package com.quittle.androidemulator;

import com.android.build.gradle.BaseExtension;
import com.android.builder.model.ApiVersion;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.gradle.api.Project;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class EmulatorConfiguration {
    /**
     * Paths to a folder containing {@code sdkmanager} relative to $SDK_ROOT.
     * {@code null} entries indicate that the
     * folder in the path should be a version number and to select the highest
     * revision possible. This
     * {@code sdkmanager} will be used to bootstrap the plugin and install a newer
     * version. The earlier entries are
     * preferred over later ones.
     */
    private static final String[][] POTENTIAL_INITIAL_SDK_MANAGER_PATHS = new String[][] {
            // This is used if cmdline-tools is downloaded separately and copied into
            // $SDK_ROOT
            new String[] { "cmdline-tools", "tools", "bin" },
            // This is used when cmdline-tools;latest is installed via sdkmanager
            new String[] { "cmdline-tools", "latest", "bin" },
            // This is used when a specific version of the cmdline-tools package is
            // installed via sdkmanager
            new String[] { "cmdline-tools", null, "bin" },
            // This is used when the sdkmanager is provided by the legacy sdk tools which
            // haven't been updated in years.
            new String[] { "tools", "bin" },
    };

    private final File sdkRoot;
    private final File avdRoot;
    private final Map<String, String> environmentVariableMap;
    private final boolean enableForAndroidTests;
    private final List<String> additionalEmulatorArguments;
    private final List<String> additionalSdkManagerArguments;
    private final boolean logEmulatorOutput;
    private final String androidVersion;
    private final String flavor;
    private final String abi;
    private final String systemImagePackageName;
    private final String emulatorName;
    private final String deviceType;
    private Integer emulatorPort;

    EmulatorConfiguration(final Project project, final BaseExtension androidExtension,
            final AndroidEmulatorExtension androidEmulatorExtension) {
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

        final Map<String, String> environmentVariableMap = new HashMap<>();
        environmentVariableMap.put("ANDROID_SDK_ROOT", sdkRoot.getAbsolutePath());
        environmentVariableMap.put("ANDROID_HOME", sdkRoot.getAbsolutePath());
        environmentVariableMap.put("ANDROID_AVD_HOME", avdRoot.getAbsolutePath());
        this.environmentVariableMap = Collections.unmodifiableMap(environmentVariableMap);

        this.enableForAndroidTests = androidEmulatorExtension.getEnableForAndroidTests();

        this.additionalEmulatorArguments = new ArrayList<>();
        if (androidEmulatorExtension.getHeadless()) {
            additionalEmulatorArguments.add("-no-skin");
            additionalEmulatorArguments.add("-no-audio");
            additionalEmulatorArguments.add("-no-window");
        }
        final String[] additionalEmulatorArgs = androidEmulatorExtension.getAdditionalEmulatorArguments();
        if (additionalEmulatorArgs != null) {
            additionalEmulatorArguments.addAll(Arrays.asList(additionalEmulatorArgs));
        }

        final String[] additionalSdkManagerArgs = androidEmulatorExtension.getAdditionalSdkManagerArguments();
        if (additionalSdkManagerArgs != null) {
            additionalSdkManagerArguments = new ArrayList<>();
            additionalSdkManagerArguments.addAll(Arrays.asList(additionalSdkManagerArgs));
        } else {
            additionalSdkManagerArguments = Collections.emptyList();
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

    private static File sdkFile(final File sdkRoot, final String... pathParts) {
        File path = sdkRoot;
        for (final String part : pathParts) {
            if (part != null) {
                path = new File(path, part);
            } else if (!path.isDirectory()) {
                return null;
            } else {
                File[] children = path.listFiles();

                if (children == null || children.length == 0) {
                    return null;
                }
                path = Arrays.stream(children)
                        .max((a, b) -> new VersionComparator().compare(a.getName(), b.getName()))
                        .orElseThrow();
            }
        }

        return path;
    }

    public File getSdkRoot() {
        return sdkRoot;
    }

    public File sdkFile(String... path) {
        return sdkFile(sdkRoot, path);
    }

    /**
     * Provides the initial {@code sdkmanager} used to install the latest version,
     * retrieved later by this plugin via {@link #getCmdLineToolsSdkManager}.
     *
     * @return The {@code sdkmanager} file location, which is guaranteed to exist
     *         and be a file if returned.
     * @throws RuntimeException if no {@code sdkmanager} to use can be found on
     *                          disk.
     */
    public File getSdkManager() throws RuntimeException {
        for (final String[] path : POTENTIAL_INITIAL_SDK_MANAGER_PATHS) {
            final File basePath = sdkFile(sdkRoot, path);
            if (basePath != null) {
                final File sdkmanager;
                if (Os.isFamily(Os.FAMILY_WINDOWS)) {
                    sdkmanager = new File(basePath, "sdkmanager.bat");
                } else {
                    sdkmanager = new File(basePath, "sdkmanager");
                }
                if (sdkmanager.isFile()) {
                    return sdkmanager;
                }
            }
        }
        throw new RuntimeException("Unable to find a valid sdkmanager to use.");
    }

    public File getCmdLineToolsSdkManager() {
        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            return sdkFile(sdkRoot, "cmdline-tools", "latest", "bin", "sdkmanager.bat");
        } else {
            return sdkFile(sdkRoot, "cmdline-tools", "latest", "bin", "sdkmanager");
        }
    }

    public File getAvdManager() {
        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            return sdkFile(sdkRoot, "cmdline-tools", "latest", "bin", "avdmanager.bat");
        } else {
            return sdkFile(sdkRoot, "cmdline-tools", "latest", "bin", "avdmanager");
        }
    }

    public File getEmulator() {
        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            return sdkFile(sdkRoot, "emulator", "emulator.exe");
        } else {
            return sdkFile(sdkRoot, "emulator", "emulator");
        }
    }

    public File getAdb() {
        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            return sdkFile(sdkRoot, "platform-tools", "adb.exe");
        } else {
            return sdkFile(sdkRoot, "platform-tools", "adb");
        }
    }

    public File getAvdRoot() {
        return avdRoot;
    }

    public Map<String, String> getEnvironmentVariableMap() {
        return environmentVariableMap;
    }

    public boolean getEnableForAndroidTests() {
        return enableForAndroidTests;
    }

    public List<String> getAdditionalEmulatorArguments() {
        return additionalEmulatorArguments;
    }

    public List<String> getAdditionalSdkManagerArguments() {
        return additionalSdkManagerArguments;
    }

    public boolean getLogEmulatorOutput() {
        return logEmulatorOutput;
    }

    public String getAndroidVersion() {
        return androidVersion;
    }

    public String getFlavor() {
        return flavor;
    }

    public String getAbi() {
        return abi;
    }

    public String getSystemImagePackageName() {
        return systemImagePackageName;
    }

    public String getEmulatorName() {
        return emulatorName;
    }

    public String getDeviceType() {
        return deviceType;
    }

    /**
     * When the plugin starts the emulator, it should bind it to a specify a port in
     * the range 5554 to 5682 and call this method to set it for other tasks to use.
     * See https://developer.android.com/studio/run/emulator-commandline#common for
     * more details.
     *
     * @param port The port to be bound to the emulator.
     */
    public void setEmulatorPort(final int port) {
        this.emulatorPort = port;
    }

    /**
     * The port the emulator was bound to in the range 5554 to 5682. Note that if
     * bound the port will always be even.
     * See https://developer.android.com/studio/run/emulator-commandline#common for
     * more details.
     *
     * @return The port the emulator should be bound to or null if not bound yet.
     */
    public Integer getEmulatorPort() {
        return this.emulatorPort;
    }
}
