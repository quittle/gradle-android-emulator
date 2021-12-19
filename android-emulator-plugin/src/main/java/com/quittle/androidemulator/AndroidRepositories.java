package com.quittle.androidemulator;

import com.android.prefs.AndroidLocationsException;
import com.android.prefs.AndroidLocationsSingleton;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Map;
import java.util.Properties;

/**
 * Manages Android repository configuration files. Does not manipulate files
 * until {@link #save} is called.
 */
public class AndroidRepositories {
    private static final String PROPERTIES_COUNT_KEY = "count";
    private static final String PROPERTIES_ENABLED_KEY = "enabled";
    private static final String PROPERTIES_SRC_KEY = "src";
    private static final String PROPERTIES_DISP_KEY = "disp";

    private final File repositoriesFile;
    private final Properties properties;

    /**
     * Loads the configuration properties file from disk. Avoid holding onto a
     * reference to this for a long time as the file may be manipulated in between.
     *
     * @return a repositories file for manipulation.
     * @throws AndroidRepositoryException if something goes wrong finding or reading
     *                                    the config file on disk.
     */
    public static AndroidRepositories load() throws AndroidRepositoryException {
        final File repositoriesFile = getRepositoriesFile();

        final Properties props = new Properties();
        if (repositoriesFile.exists()) {
            try (final InputStream is = new FileInputStream(repositoriesFile)) {
                props.load(is);
            } catch (final IOException e) {
                throw new AndroidRepositoryException("Unable to read repositories.cfg file", e);
            }
        }

        return new AndroidRepositories(repositoriesFile, props);
    }

    /**
     * Writes updates to the file on disk. May be called multiple times and
     * overrides, rather than updates, the existing file.
     *
     * @throws AndroidRepositoryException if unable to write the update.
     */
    public void save() throws AndroidRepositoryException {
        try (final OutputStream os = new FileOutputStream(this.repositoriesFile)) {
            this.properties.store(os, "Last updated by the com.quittle.android-emulator Gradle plugin");
        } catch (final IOException e) {
            throw new AndroidRepositoryException("Unable to save repositories.cfg", e);
        }
    }

    public void addRepository(final String friendlyName, final URL url) throws AndroidRepositoryException {
        final int curCount;
        try {
            curCount = Integer.parseInt(properties.getProperty(PROPERTIES_COUNT_KEY, "0"), 10);
        } catch (final NumberFormatException e) {
            throw new AndroidRepositoryException("Unable to parse count key of repositories.cfg", e);
        }

        final String urlString = url.toString();

        for (final Map.Entry<Object, Object> entry : properties.entrySet()) {
            final String key = (String) entry.getKey();
            final String value = (String) entry.getValue();

            if (key.startsWith(PROPERTIES_SRC_KEY) && value.equals(urlString)) {
                final int index;
                try {
                    index = Integer.parseInt(key.substring(3), 10);
                } catch (final NumberFormatException e) {
                    throw new AndroidRepositoryException("Unable to parse index of repositories.cfg key: " + key, e);
                }

                properties.setProperty(buildPropertyIndexedName(PROPERTIES_ENABLED_KEY, index), "true");
                return;
            }
        }

        properties.setProperty(buildPropertyIndexedName(PROPERTIES_ENABLED_KEY, curCount), "true");
        properties.setProperty(buildPropertyIndexedName(PROPERTIES_DISP_KEY, curCount), friendlyName);
        properties.setProperty(buildPropertyIndexedName(PROPERTIES_SRC_KEY, curCount), urlString);
        properties.setProperty(PROPERTIES_COUNT_KEY, String.valueOf(curCount + 1));
    }

    /**
     * The location of the repositories configuration file used by the Android SDK
     * tools. This file may not exist.
     *
     * @return The configuration file location.
     * @throws AndroidRepositoryException if unable to determine its location.
     */
    @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "Broad catch to handle both reflection exceptions and AndroidLocationExceptions that would need to be loaded with reflection anyway")
    public static File getRepositoriesFile() throws AndroidRepositoryException {
        final String errorMessage = "Unable to find android home directory";
        try {
            try {
                return AndroidLocationsSingleton.INSTANCE.getPrefsLocation().toFile();
            } catch (final AndroidLocationsException e) {
                throw new AndroidRepositoryException(errorMessage, e);
            }
        } catch (final NoClassDefFoundError _e) {
            // This is expected to occur when running with older versions of the plugin

            try {
                final Class<?> clazz = Class.forName("com.android.prefs.AndroidLocation");
                final Method method = clazz.getDeclaredMethod("getFolder");
                final String folder = (String) method.invoke(null);
                return new File(folder, "repositories.cfg");
            } catch (final Exception e) {
                // This handles all manner of reflection, casting, and exceptions that could be
                // thrown by calling getFolder()
                throw new AndroidRepositoryException("Unable to find android home directory", e);
            }
        }
    }

    /**
     * Source:
     * https://android.googlesource.com/platform/sdk/+/tools_r21/sdkmanager/libs/sdklib/src/com/android/sdklib/internal/repository/sources/SdkSources.java#289
     */
    private static String buildPropertyIndexedName(final String key, final int index) {
        return String.format("%s%02d", key, index);
    }

    private AndroidRepositories(final File repositoriesFile, final Properties properties) {
        this.repositoriesFile = repositoriesFile;
        this.properties = properties;
    }
}
