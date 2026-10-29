package com.bebis.BeBiS.profile.domain;

import com.bebis.BeBiS.equipment.domain.Equipment;

import static com.bebis.BeBiS.profile.domain.ClassSpec.getClassSpec;

/**
 * A selected character with its synced equipment and talents: everything the upgrade engine needs.
 */
public record CharacterInfo(
        WowCharacter wowCharacter,
        Equipment equipment,
        WowTalents talents
) {

    /**
     * Resolved from the talents on every call, see {@link ClassSpec#getClassSpec}.
     */
    public ClassSpec classSpec() {
        return getClassSpec(wowCharacter().wowClass(), talents());
    }

    public WowCharacter.Race race() {
        return wowCharacter().race();
    }

    public int level() {
        return wowCharacter().level();
    }

    public WowCharacter.WowClass wowClass() {
        return wowCharacter().wowClass();
    }
}
