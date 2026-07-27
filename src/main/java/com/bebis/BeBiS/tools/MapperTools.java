package com.bebis.BeBiS.tools;

import java.util.function.Function;

public class MapperTools {

    private MapperTools() {
    }

    public static <T, E extends RuntimeException> T validateRequired(T value, String fieldName, Function<String, E> exception) {
        if (value == null) {
            throw exception.apply("Critical data missing: " + fieldName);
        }
        return value;
    }
}
