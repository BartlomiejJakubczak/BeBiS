package com.bebis.BeBiS.integration.blizzard.dto;

import java.util.List;

public record WowAccountDTO(
        long id,
        List<WowCharacterDTO> characters
) {
}
