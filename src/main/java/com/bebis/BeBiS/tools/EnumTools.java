package com.bebis.BeBiS.tools;

import java.util.Optional;

public class EnumTools {

    private EnumTools() {
    }

    public static <E extends Enum<E>> Optional<E> fromString(Class<E> enumClass, String value) {
        try {
            return Optional.of(Enum.valueOf(enumClass, value.toUpperCase()));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }
}
