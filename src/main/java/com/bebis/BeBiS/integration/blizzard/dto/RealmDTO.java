package com.bebis.BeBiS.integration.blizzard.dto;

public record RealmDTO(
        int id,
        String name,
        String slug,
        HrefLinkDTO key
) {
}
