package com.quittle.androidemulator;

import com.android.build.gradle.BaseExtension;
import com.android.build.gradle.internal.dsl.DefaultConfig;
import io.mockk.impl.annotations.MockK;
import io.mockk.junit5.MockKExtension;
import org.gradle.api.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;

import static io.mockk.MockKKt.every;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SuppressWarnings({ "PMD.AvoidDuplicateLiterals" })
@ExtendWith({ MockitoExtension.class, MockKExtension.class })
class EmulatorConfigurationTest {
    @Mock
    private Project mockProject;
    @MockK
    private BaseExtension mockBaseExtension;
    @Mock
    private AndroidEmulatorExtension mockAndroidEmulatorExtension;
    @Mock
    private AndroidEmulatorExtension.EmulatorExtension mockEmulatorExtension;
    @Mock
    private DefaultConfig mockDefaultConfig;
    @TempDir
    File tempDir;

    private EmulatorConfiguration configuration;

    @BeforeEach
    void setUp() {
        every(_scope -> mockBaseExtension.getSdkDirectory()).returns(tempDir);
        every(_scope -> mockBaseExtension.getDefaultConfig()).returns(mockDefaultConfig);
        when(mockAndroidEmulatorExtension.getEmulator()).thenReturn(mockEmulatorExtension);
        configuration = new EmulatorConfiguration(mockProject, mockBaseExtension, mockAndroidEmulatorExtension);
    }

    @Test
    void testGetSdkManager_emptySdkRoot() {
        assertGetSdkManagerThrows();
    }

    @Test
    void testGetSdkManager_unrelatedFolder() {
        makeSdkmanagerInTempDirectory("foo");
        assertGetSdkManagerThrows();
    }

    /**
     * Commandline tools downloaded standalone and placed in SDK_ROOT
     */
    @Test
    void testGetSdkManager_cmdlineToolsTools() {
        final File file = makeSdkmanagerInTempDirectory("cmdline-tools", "tools", "bin");
        assertEquals(file, configuration.getSdkManager());
    }

    /**
     * Commandline tools installed by sdkmanager installed as "latest" version
     */
    @Test
    void testGetSdkManager_cmdlineToolsLatest() {
        final File file = makeSdkmanagerInTempDirectory("cmdline-tools", "latest", "bin");
        assertEquals(file, configuration.getSdkManager());
    }

    /**
     * Commandline tools installed by sdkmanager installed as a specific version
     */
    @Test
    void testGetSdkManager_cmdlineToolsVersion() {
        final File file = makeSdkmanagerInTempDirectory("cmdline-tools", "2.1", "bin");
        assertEquals(file, configuration.getSdkManager());
    }

    /**
     * Usecase for original sdk tools
     */
    @Test
    void testGetSdkManager_tools() {
        final File file = makeSdkmanagerInTempDirectory("tools", "bin");
        assertEquals(file, configuration.getSdkManager());
    }

    @Test
    void testGetSdkManager_allowInvalidVersions() {
        File sdkmanager = makeSdkmanagerInTempDirectory("cmdline-tools", "madeupversion", "bin");
        assertEquals(sdkmanager, configuration.getSdkManager());
    }

    @Test
    void testGetSdkManager_preferValidVersions() {
        // Despite having many options for versions, only the valid version should be
        // chosen
        makeSdkmanagerInTempDirectory("cmdline-tools", "invalid", "bin");
        makeSdkmanagerInTempDirectory("cmdline-tools", "0invalid", "bin");
        makeSdkmanagerInTempDirectory("cmdline-tools", "1invalid", "bin");
        makeSdkmanagerInTempDirectory("cmdline-tools", "1.invalid", "bin");
        makeSdkmanagerInTempDirectory("cmdline-tools", "1-tagged", "bin");
        final File sdkmanager = makeSdkmanagerInTempDirectory("cmdline-tools", "1.0", "bin");
        makeSdkmanagerInTempDirectory("cmdline-tools", "2invalid", "bin");

        assertEquals(sdkmanager, configuration.getSdkManager());
    }

    @Test
    void testGetSdkManager_versionOrderOfPrecedense() {
        final File version1 = makeSdkmanagerInTempDirectory("cmdline-tools", "1", "bin");
        final File version2 = makeSdkmanagerInTempDirectory("cmdline-tools", "2.1", "bin");
        final File version3 = makeSdkmanagerInTempDirectory("cmdline-tools", "3", "bin");
        final File version10 = makeSdkmanagerInTempDirectory("cmdline-tools", "10.0.1", "bin");

        // Verify their precedence by deleting them in order
        assertEquals(version10, configuration.getSdkManager());
        deleteFile(version10);
        assertEquals(version3, configuration.getSdkManager());
        deleteFile(version3);
        assertEquals(version2, configuration.getSdkManager());
        deleteFile(version2);
        assertEquals(version1, configuration.getSdkManager());
        deleteFile(version1);
        assertGetSdkManagerThrows();
    }

    @Test
    void testGetSdkManager_orderOfVersionPrecedence() {
        // These should each override eachotehr
        final File cmdlineTools = makeSdkmanagerInTempDirectory("cmdline-tools", "tools", "bin");
        final File cmdlineLatest = makeSdkmanagerInTempDirectory("cmdline-tools", "latest", "bin");
        final File cmdlineVersion = makeSdkmanagerInTempDirectory("cmdline-tools", "2.1", "bin");
        final File legacy = makeSdkmanagerInTempDirectory("tools", "bin");

        // Verify their precedence by deleting them in order
        assertEquals(cmdlineTools, configuration.getSdkManager());
        deleteFile(cmdlineTools);

        assertEquals(cmdlineLatest, configuration.getSdkManager());
        deleteFile(cmdlineLatest);

        assertEquals(cmdlineVersion, configuration.getSdkManager());
        deleteFile(cmdlineVersion);

        assertEquals(legacy, configuration.getSdkManager());
        deleteFile(legacy);

        assertGetSdkManagerThrows();
    }

    /**
     * Asserts that calling {@link EmulatorConfiguration#getSdkManager()} throws an
     * exception.
     */
    private void assertGetSdkManagerThrows() {
        final RuntimeException exception = assertThrows(RuntimeException.class, () -> configuration.getSdkManager());
        assertEquals("Unable to find a valid sdkmanager to use.", exception.getMessage());
    }

    /**
     * Given a folder path, generates a synthetic {@code sdkmanager} relative to
     */
    private File makeSdkmanagerInTempDirectory(String... path) {
        final File parentFile = new File(tempDir, String.join(File.separator, path));
        assertTrue(parentFile.mkdirs());
        final File sdkmanager = new File(parentFile, OS.WINDOWS.isCurrentOs() ? "sdkmanager.bat" : "sdkmanager");
        try {
            assertTrue(sdkmanager.createNewFile());
            return sdkmanager;
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Deletes a file and its parent directories recursively. Deletes up the tree as
     * long as there are no other children.
     *.
     * @param file The file to delete
     */
    private static void deleteFile(final File file) {
        assertTrue(file.delete());
        final File parent = file.getParentFile();
        if (parent != null) {
            final String[] children = parent.list();
            if (children != null && children.length == 0) {
                deleteFile(parent);
            }
        }
    }
}
