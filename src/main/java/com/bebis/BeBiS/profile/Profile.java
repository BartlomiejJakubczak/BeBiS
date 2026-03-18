package com.bebis.BeBiS.profile;

import java.util.List;

public record Profile(
        long id,
        List<WowCharacter> characters
) {
}
