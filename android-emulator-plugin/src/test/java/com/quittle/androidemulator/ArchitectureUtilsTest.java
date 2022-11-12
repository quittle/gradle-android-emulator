package com.quittle.androidemulator;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

class ArchitectureUtilsTest {
    @ParameterizedTest
    @EnumSource(ArchitectureUtils.Architecture.class)
    public void testArchitecturesValid(ArchitectureUtils.Architecture architecture) {
        assertTrue(architecture.allValues.contains(architecture.emulatorArchitecture));

        final Set<String> otherArchitectures = Stream.of(ArchitectureUtils.Architecture.values())
                .filter(otherArchitecture -> otherArchitecture != architecture)
                .flatMap(otherArchitecture -> otherArchitecture.allValues.stream())
                .collect(Collectors.toUnmodifiableSet());
        for (final String abi : architecture.allValues) {
            assertFalse(otherArchitectures.contains(abi));
        }
    }

    @Test
    public void testGetEmulatorAbiString() {
        final String abi = ArchitectureUtils.getEmulatorAbiString();
        assertNotNull(abi);
        final Set<String> allAbis = Stream.of(ArchitectureUtils.Architecture.values())
                .flatMap(architecture -> architecture.allValues.stream()).collect(Collectors.toUnmodifiableSet());
        assertTrue(allAbis.contains(abi));
    }
}
