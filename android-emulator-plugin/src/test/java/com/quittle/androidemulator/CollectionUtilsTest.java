package com.quittle.androidemulator;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.quittle.androidemulator.CollectionUtils.mutableListOf;
import static org.junit.jupiter.api.Assertions.*;

class CollectionUtilsTest {
    @Test
    public void testMutableListOf() {
        assertEquals(Collections.emptyList(), mutableListOf());

        final List<Integer> list = mutableListOf(1, 2, 3);
        assertEquals(Arrays.asList(1, 2, 3), list);
        assertDoesNotThrow(() -> list.add(4));
        assertEquals(Arrays.asList(1, 2, 3, 4), list);
    }
}
