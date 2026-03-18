package com.bebis.BeBiS.profile;

public record WowCharacter(
        long id,
        String name,
        int level,
        Race race,
        WowClass wowClass
) {
    public enum Race {
        HUMAN,
        DWARF,
        NIGHT_ELF,
        GNOME,
        ORC,
        TROLL,
        TAUREN,
        UNDEAD
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
}


