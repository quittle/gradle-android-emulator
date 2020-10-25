package com.quittle.androidemulator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class CollectionUtils {
    /**
     * Converts the arguments to mutable list. This is preferable to {@link Arrays#asList} when a mutable list is
     * required.
     * @param <T> The type of the contents of the list
     * @param values The values to be part of the list
     * @return A mutable list of unspecified type.
     */
    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <T> List<T> mutableListOf(T... values) {
        return new ArrayList<>(Arrays.asList(values));
    }

    private CollectionUtils() {}
}
