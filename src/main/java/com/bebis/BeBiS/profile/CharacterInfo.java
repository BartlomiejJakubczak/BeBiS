package com.bebis.BeBiS.profile;

import com.bebis.BeBiS.equipment.Equipment;

public record CharacterInfo(
        WowCharacter wowCharacter,
        Equipment equipment
) {
}
