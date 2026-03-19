package com.bebis.BeBiS.profile;

import com.bebis.BeBiS.integration.blizzard.dto.WowCharacterDTO;

public record WowCharacter(
        long id, // id in the DB should probably consist of id + slug, as id is unique per realm only
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

    public static WowCharacter fromDTO(WowCharacterDTO dto) {
        return new WowCharacter(
                dto.id(),
                dto.name(),
                dto.level(),
                Race.valueOf(dto.race().name()),
                WowClass.valueOf(dto.wowClass().name()),
                new WowRealm(dto.realm().name(), dto.realm().slug())
        );
    }

}


