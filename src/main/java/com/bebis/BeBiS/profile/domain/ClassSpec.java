package com.bebis.BeBiS.profile.domain;

import com.bebis.BeBiS.profile.domain.WowCharacter.WowClass;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public enum ClassSpec {

    WARRIOR_FURY(WowClass.WARRIOR, "FURY"),
    WARRIOR_ARMS(WowClass.WARRIOR, "ARMS"),
    WARRIOR_PROTECTION(WowClass.WARRIOR, "PROTECTION"),
    PALADIN_HOLY(WowClass.PALADIN, "HOLY"),
    PALADIN_RETRIBUTION(WowClass.PALADIN, "RETRIBUTION"),
    PALADIN_PROTECTION(WowClass.PALADIN, "PROTECTION"),
    ROGUE_ASSASSINATION(WowClass.ROGUE, "ASSASSINATION"),
    ROGUE_COMBAT(WowClass.ROGUE, "COMBAT"),
    ROGUE_SUBTLETY(WowClass.ROGUE, "SUBTLETY"),
    SHAMAN_ENHANCEMENT(WowClass.SHAMAN, "ENHANCEMENT"),
    SHAMAN_ELEMENTAL(WowClass.SHAMAN, "ELEMENTAL"),
    SHAMAN_RESTORATION(WowClass.SHAMAN, "RESTORATION"),
    DRUID_FERAL(WowClass.DRUID, "FERAL"),
    DRUID_RESTORATION(WowClass.DRUID, "RESTORATION"),
    DRUID_BALANCE(WowClass.DRUID, "BALANCE"),
    HUNTER_BEASTMASTERY(WowClass.HUNTER, "BEASTMASTERY"),
    HUNTER_MARKSMANSHIP(WowClass.HUNTER, "MARKSMANSHIP"),
    HUNTER_SURVIVAL(WowClass.HUNTER, "SURVIVAL"),
    PRIEST_HOLY(WowClass.PRIEST, "HOLY"),
    PRIEST_DISCIPLINE(WowClass.PRIEST, "DISCIPLINE"),
    PRIEST_SHADOW(WowClass.PRIEST, "SHADOW"),
    MAGE_FROST(WowClass.MAGE, "FROST"),
    MAGE_FIRE(WowClass.MAGE, "FIRE"),
    MAGE_ARCANE(WowClass.MAGE, "ARCANE"),
    WARLOCK_DESTRUCTION(WowClass.WARLOCK, "DESTRUCTION"),
    WARLOCK_AFFLICTION(WowClass.WARLOCK, "AFFLICTION"),
    WARLOCK_DEMONOLOGY(WowClass.WARLOCK, "DEMONOLOGY"),
    WARRIOR_NONE(WowClass.WARRIOR, "NONE"),
    PALADIN_NONE(WowClass.PALADIN, "NONE"),
    ROGUE_NONE(WowClass.ROGUE, "NONE"),
    SHAMAN_NONE(WowClass.SHAMAN, "NONE"),
    DRUID_NONE(WowClass.DRUID, "NONE"),
    HUNTER_NONE(WowClass.HUNTER, "NONE"),
    PRIEST_NONE(WowClass.PRIEST, "NONE"),
    MAGE_NONE(WowClass.MAGE, "NONE"),
    WARLOCK_NONE(WowClass.WARLOCK, "NONE");

    private final WowClass wowClass;
    private final String specName;

    private final static Map<String, ClassSpec> LOOKUP_TABLE;

    static {
        LOOKUP_TABLE = Arrays.stream(ClassSpec.values())
                .collect(Collectors.toMap(
                        (classSpec -> classSpec.wowClass + "_" + classSpec.specName),
                        (classSpec -> classSpec)
                ));
    }

    ClassSpec(WowClass wowClass, String specName) {
        this.wowClass = wowClass;
        this.specName = specName;
    }

    public static ClassSpec getClassSpec(WowClass wowClass, Optional<WowTalents> talents) {
        String specName = talents.map(WowTalents::getActiveSpec).orElse(WowTalents.NO_SPEC);
        ClassSpec spec = LOOKUP_TABLE.get(wowClass + "_" + specName);
        return (spec != null) ? spec : ClassSpec.valueOf(wowClass + "_NONE");
    }

}
