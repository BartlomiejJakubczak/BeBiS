package com.bebis.BeBiS.profile;

import com.bebis.BeBiS.integration.blizzard.dto.WowCharacterDTO;

public record WowCharacter(
        Id id,
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

    public record Id(
            long id,
            String realmSlug
    ) {
    }

    public static WowCharacter fromDTO(WowCharacterDTO dto) {
        return new WowCharacter(
                new Id(dto.id(), dto.realm().slug()),
                dto.name(),
                dto.level(),
                Race.valueOf(dto.race().name()),
                WowClass.valueOf(dto.wowClass().name()),
                new WowRealm(dto.realm().name(), dto.realm().slug())
        );
    }

}


