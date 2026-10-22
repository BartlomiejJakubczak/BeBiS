package com.bebis.BeBiS.tools;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

public final class EnumTools {

    private EnumTools() {
    }

    public static <E extends Enum<E>> Optional<E> fromString(Class<E> enumClass, String value) {
        try {
            return Optional.of(Enum.valueOf(enumClass, value.toUpperCase()));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    /**
     * Finds the constant with the given game id, e.g. {@code fromId(WowClass.class, 1)} is {@code WARRIOR}.
     */
    public static <E extends Enum<E> & GameIdentified> Optional<E> fromId(Class<E> enumClass, int id) {
        return Arrays.stream(enumClass.getEnumConstants())
                .filter(constant -> constant.getId() == id)
                .findFirst();
    }

    /**
     * Decodes a game bitmask in which bit {@code id - 1} marks a member, e.g. the race mask {@code 77}
     * ({@code 0b0100_1101}) is Human (1), Dwarf (3), Night Elf (4) and Gnome (7). Bits without a matching constant are
     * ignored. A mask of {@code 0} decodes to an empty set, which callers treat as "no restriction".
     */
    public static <E extends Enum<E> & GameIdentified> Set<E> fromBitmask(Class<E> enumClass, int mask) {
        EnumSet<E> members = EnumSet.noneOf(enumClass);
        for (E constant : enumClass.getEnumConstants()) {
            if ((mask & (1 << (constant.getId() - 1))) != 0) {
                members.add(constant);
            }
        }
        return members;
    }
}
