package com.quittle.androidemulator;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;

public class VersionComparator implements Comparator<String>, Serializable {
    private static final long serialVersionUID = 0;

    @Override
    public int compare(String a, String b) {
        final int[] aParts;
        try {
            aParts = stringToVersion(a);
        } catch (NumberFormatException _e) {
            return -1;
        }

        final int[] bParts;
        try {
            bParts = stringToVersion(b);
        } catch (NumberFormatException _e) {
            return 1;
        }

        for (int i = 0; i < aParts.length; i++) {
            if (i >= bParts.length) {
                return 1;
            }

            final int aVersion = aParts[i];
            final int bVersion = bParts[i];

            if (aVersion > bVersion) {
                return 1;
            } else if (aVersion < bVersion) {
                return -1;
            }
        }

        if (bParts.length > aParts.length) {
            return -1;
        }

        return 0;
    }

    private static int[] stringToVersion(String version) {
        final String[] versionParts = version.split("\\.");
        return Arrays.stream(versionParts)
                .mapToInt(Integer::parseInt)
                .toArray();
    }
}
