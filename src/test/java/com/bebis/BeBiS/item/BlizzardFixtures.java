package com.bebis.BeBiS.item;

import com.bebis.BeBiS.integration.blizzard.dto.ItemResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/**
 * Loads responses recorded from the real Blizzard API (src/test/resources/blizzard).
 * Builders encode our assumptions about the API, these fixtures encode what it actually sends.
 */
public final class BlizzardFixtures {

    // Same defaults as the ObjectMapper behind the app's RestClient (e.g. unknown JSON properties are ignored)
    private static final ObjectMapper BLIZZARD_JSON = Jackson2ObjectMapperBuilder.json().build();

    private BlizzardFixtures() {
    }

    public static ItemResponse loadItem(String fileName) {
        String path = "/blizzard/item/" + fileName;
        try (InputStream json = BlizzardFixtures.class.getResourceAsStream(path)) {
            if (json == null) {
                throw new IllegalArgumentException("Missing fixture " + path);
            }
            return BLIZZARD_JSON.readValue(json, ItemResponse.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
