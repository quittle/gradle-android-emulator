package com.quittle.androidemulator;

import org.gradle.api.Action;

import java.io.File;

/**
 * Provides configuration for {@link AndroidEmulatorPlugin}.
 */
@SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
public class AndroidEmulatorExtension {
    public static class EmulatorExtension {
        private String name = null;
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
}