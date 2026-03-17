package com.bebis.BeBiS.integration.blizzard.dto;

import java.util.List;

public record WowAccount(
        long id,
        List<WowCharacter> characters
) {
}
