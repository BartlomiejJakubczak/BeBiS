package com.bebis.BeBiS.profile.domain;

import com.bebis.BeBiS.equipment.domain.Equipment;

import java.util.Optional;

public record CharacterInfo(
        WowCharacter wowCharacter,
        Equipment equipment,
        Optional<WowTalents> talents
) {
}
