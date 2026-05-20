package com.bebis.BeBiS.profile.domain;

import com.bebis.BeBiS.equipment.domain.Equipment;

public record CharacterInfo(
        WowCharacter wowCharacter,
        Equipment equipment
) {
}
