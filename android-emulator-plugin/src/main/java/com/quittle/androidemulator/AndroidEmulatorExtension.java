package com.quittle.androidemulator;

import org.gradle.api.Action;

import java.io.File;
import java.util.Collection;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Provides configuration for {@link AndroidEmulatorPlugin}.
 */
@SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
public class AndroidEmulatorExtension {
    public static class EmulatorExtension {
        private String name = null;
        private String device = null;
        private int sdkVersion = -1;
        private String abi = "x86";
        private boolean includeGoogleApis = false;

        public String getName() {
            return this.name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public void name(final String name) {
            this.name = name;
        }

        public String getDevice() {
            return this.device;
        }

        public void setDevice(final String device) {
            this.device = device;
        }

        public void device(final String device) {
            this.device = device;
        }

        public int getSdkVersion() {
            return this.sdkVersion;
        }

        public void setSdkVersion(final int sdkVersion) {
            this.sdkVersion = sdkVersion;
        }

        public void sdkVersion(final int sdkVersion) {
            this.sdkVersion = sdkVersion;
        }

        public String getAbi() {
            return this.abi;
        }

        public void setAbi(final String abi) {
            this.abi = abi;
        }

        public void abi(final String abi) {
            this.abi = abi;
        }

        public boolean getIncludeGoogleApis() {
            return this.includeGoogleApis;
        }

        public void setIncludeGoogleApis(final boolean includeGoogleApis) {
            this.includeGoogleApis = includeGoogleApis;
        }

        public void includeGoogleApis(final boolean includeGoogleApis) {
            this.includeGoogleApis = includeGoogleApis;
        }
    }

    private final EmulatorExtension emulator = new EmulatorExtension();
    private File avdRoot = null;
    private boolean enableForAndroidTests = true;
    private boolean headless = false;
    private String[] additionalEmulatorArguments = null;
    private boolean logEmulatorOutput = false;

    public EmulatorExtension getEmulator() {
        return this.emulator;
    }

    public void emulator(Action<EmulatorExtension> action) {
        action.execute(this.emulator);
    }

    public void setAvdRoot(final File avdRoot) {
        this.avdRoot = avdRoot;
    }

    public void avdRoot(final File avdRoot) {
        this.avdRoot = avdRoot;
    }

    public File getAvdRoot() {
        return this.avdRoot;
    }

    public void setEnableForAndroidTests(final boolean enableForAndroidTests) {
        this.enableForAndroidTests = enableForAndroidTests;
    }

    public void enableForAndroidTests(final boolean enableForAndroidTests) {
        this.enableForAndroidTests = enableForAndroidTests;
    }

    public boolean getEnableForAndroidTests() {
        return this.enableForAndroidTests;
    }

    public void headless(final boolean headless) {
        this.headless = headless;
    }

    public void setHeadless(final boolean headless) {
        this.headless = headless;
    }

    public boolean getHeadless() {
        return this.headless;
    }

    public void additionalEmulatorArguments(final String[] additionalEmulatorArguments) {
        this.additionalEmulatorArguments = clone(additionalEmulatorArguments);
    }

    public void additionalEmulatorArguments(final Collection<String> additionalEmulatorArguments) {
        this.additionalEmulatorArguments = toArray(additionalEmulatorArguments);
    }

    public void setAdditionalEmulatorArguments(final String[] additionalEmulatorArguments) {
        this.additionalEmulatorArguments = clone(additionalEmulatorArguments);
    }

    public void setAdditionalEmulatorArguments(final Collection<String> additionalEmulatorArguments) {
        this.additionalEmulatorArguments = toArray(additionalEmulatorArguments);
    }

    public String[] getAdditionalEmulatorArguments() {
        return clone(this.additionalEmulatorArguments);
    }

    public void logEmulatorOutput(final boolean logEmulatorOutput) {
        this.logEmulatorOutput = logEmulatorOutput;
    }

    public void setLogEmulatorOutput(final boolean logEmulatorOutput) {
        this.logEmulatorOutput = logEmulatorOutput;
    }

    public boolean getLogEmulatorOutput() {
        return this.logEmulatorOutput;
    }

    /**
     * Helper method for cloning a potentially null array
     *
     * @param arr The array to clone
     * @param <T> The type of array
     * @return {@code null} if {@code arr} is {@code null}, otherwise a copy of the
     *         input array
     */
    @SuppressFBWarnings("PZLA_PREFER_ZERO_LENGTH_ARRAYS")
    private static <T> T[] clone(final T[] arr) {
        if (arr == null) {
            return null;
        }
        return arr.clone();
    }

    /**
     * Helper method for converting a collection to an array
     *
     * @param collection The array to convert
     * @return {@code null} if {@code collection} is {@code null}, otherwise an
     *         array containing the
     *         contents of {@code collection}.
     */
    @SuppressFBWarnings("PZLA_PREFER_ZERO_LENGTH_ARRAYS")
    private static String[] toArray(final Collection<String> collection) {
        if (collection == null) {
            return null;
        }

        return collection.toArray(new String[collection.size()]);
    }
}