package com.bebis.BeBiS.profile.domain;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

public record WowCharacter(
        Id wowCharacterId,
        String name,
        int level,
        Race race,
        WowClass wowClass,
        WowRealm realm
) {
    public enum Race {
        HUMAN(1),
        ORC(2),
        DWARF(3),
        NIGHT_ELF(4),
        UNDEAD(5),
        TAUREN(6),
        GNOME(7),
        TROLL(8);

        @Getter
        private final int id;

        Race(int id) {
            this.id = id;
        }

        public static Optional<Race> fromId(int id) {
            return Arrays.stream(Race.values()).filter(x -> x.getId() == id).findFirst();
        }
    }

    public enum WowClass {
        WARRIOR(1),
        PALADIN(2),
        HUNTER(3),
        ROGUE(4),
        PRIEST(5),
        SHAMAN(7),
        MAGE(8),
        WARLOCK(9),
        DRUID(11);

        @Getter
        private final int id;

        WowClass(int id) {
            this.id = id;
        }

        public static Optional<WowClass> fromId(int id) {
            return Arrays.stream(WowClass.values()).filter(x -> x.getId() == id).findFirst();
        }
    }

    public record Id(
            long id,
            String realmSlug
    ) {
    }

}


