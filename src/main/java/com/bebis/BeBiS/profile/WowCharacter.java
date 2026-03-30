package com.bebis.BeBiS.profile;

public record WowCharacter(
        Id wowCharacterId,
        String name,
        int level,
        Race race,
        WowClass wowClass,
        WowRealm realm
) {
    public enum Race {
        HUMAN,
        DWARF,
        NIGHT_ELF,
        GNOME,
        ORC,
        TROLL,
        TAUREN,
        UNDEAD;

        public static Race fromBlizzardName(String blizzardName) {
            return Race.valueOf(blizzardName.toUpperCase().replace("-", "_"));
        }
    }

    public enum WowClass {
        WARRIOR,
        ROGUE,
        PALADIN,
        SHAMAN,
        PRIEST,
        MAGE,
        WARLOCK,
        HUNTER,
        DRUID
    }

    public record Id(
            long id,
            String realmSlug
    ) {
    }

}


