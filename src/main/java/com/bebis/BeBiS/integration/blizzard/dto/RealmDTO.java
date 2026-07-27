package com.bebis.BeBiS.integration.blizzard.dto;

public record RealmDTO(
        Integer id,
        String name,
        String slug,
        HrefLinkDTO key
) {
}
