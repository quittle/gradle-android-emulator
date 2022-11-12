package com.quittle.androidemulator;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ArchitectureUtils {
    public enum Architecture {
        ARCH_ARMEABI_V7A("armeabi-v7a", "arm-v7", "armv7", "arm", "arm32"),
        ARCH_ARMEABI("armeabi"),
        ARCH_ARM64_V8A("arm64-v8a", "arm64", "aarch64"),
        ARCH_X86("x86", "i386", "ia-32", "i686"),
        ARCH_X86_64("x86_64", "amd64", "x64", "x86-64", "ia-64", "ia64");

        public final String emulatorArchitecture;
        public final Set<String> allValues;

        Architecture(final String emulatorArchitecture, final String... rest) {
            this.emulatorArchitecture = emulatorArchitecture;
            final Set<String> values = new HashSet<>(Arrays.asList(rest));
            values.add(emulatorArchitecture);
            this.allValues = values;
        }
    }

    public static  String getEmulatorAbiString() {
        final String osArch = System.getProperty("os.arch");
        for (final Architecture architecture : Architecture.values()) {
            if (architecture.allValues.contains(osArch)) {
                return architecture.emulatorArchitecture;
            }
        }
        return null;
    }

}
