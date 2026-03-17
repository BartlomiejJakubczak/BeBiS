package com.bebis.BeBiS.integration.blizzard.dto;

public record Realm(
        int id,
        String name,
        String slug,
        HrefLink key
) {
}
