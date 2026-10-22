package com.bebis.BeBiS.profile.domain;

import com.bebis.BeBiS.tools.EnumTools;
import com.bebis.BeBiS.tools.GameIdentified;
import lombok.Getter;

import java.util.Optional;

public record WowCharacter(
        Id id,
        String name,
        int level,
        Race race,
        WowClass wowClass,
        WowRealm realm
) {
    /**
     * Playable races, identified by their game id (see {@link GameIdentified}).
     */
    public enum Race implements GameIdentified {
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

        /**
         * @return the race with this game id, empty if none exists in Classic
         */
        public static Optional<Race> fromId(int id) {
            return EnumTools.fromId(Race.class, id);
        }
    }

    /**
     * Playable classes, identified by their game id (see {@link GameIdentified}). Ids 6 and 10 don't exist in Classic.
     */
    public enum WowClass implements GameIdentified {
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

        /**
         * @return the class with this game id, empty if none exists in Classic (e.g. 6, Death Knight)
         */
        public static Optional<WowClass> fromId(int id) {
            return EnumTools.fromId(WowClass.class, id);
        }
    }

    /**
     * A character's identity: Blizzard's character id is only unique within a realm, and the character belongs to one
     * Battle.net account.
     */
    public record Id(
            long id,
            String realmSlug,
            long blizzardAccountId
    ) {
    }

}


