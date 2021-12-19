package com.quittle.androidemulator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Comparator;

import org.junit.jupiter.api.Test;

public class VersionComparatorTest {
    @Test
    void testCompare() {
        final Comparator<String> comparator = new VersionComparator();

        assertEquals(comparator.compare("1.2.3", "1.2.3.4"), -1);
        assertEquals(comparator.compare("1.2.4", "1.2.3.4"), 1);
        assertEquals(comparator.compare("1.2.4", "1"), 1);
        assertEquals(comparator.compare("1.2.3", "1.3.0"), -1);
        assertEquals(comparator.compare("1.2", "1.2"), 0);
    }

    @Test
    void testCompareInvalid() {
        final Comparator<String> comparator = new VersionComparator();

        assertEquals(comparator.compare("1.invalid", "1.2"), -1);
        assertEquals(comparator.compare("invalid", "1.3"), -1);
        assertEquals(comparator.compare("1", "1.2.3.invalid"), 1);
    }
}
