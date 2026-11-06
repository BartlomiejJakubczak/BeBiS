package com.bebis.BeBiS.integration.blizzard;

import com.bebis.BeBiS.integration.blizzard.dto.ItemResponse;
import com.bebis.BeBiS.integration.blizzard.dto.SpecializationResponse;
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
    // Because we're dealing with Blizzard JSONs here it's important to parse them similarly like in production.
    private static final ObjectMapper BLIZZARD_JSON = Jackson2ObjectMapperBuilder.json().build();

    private BlizzardFixtures() {
    }

    public static ItemResponse loadItem(String fileName) {
        return load("/blizzard/item/" + fileName, ItemResponse.class);
    }

    public static ItemResponse loadBenchmarkItem(long id) {
        return load("/blizzard/item/benchmark/" + id + ".json", ItemResponse.class);
    }

    public static SpecializationResponse loadSpecialization(String fileName) {
        return load("/blizzard/specs/" + fileName, SpecializationResponse.class);
    }

    private static <T> T load(String path, Class<T> type) {
        try (InputStream json = BlizzardFixtures.class.getResourceAsStream(path)) {
            if (json == null) {
                throw new IllegalArgumentException("Missing fixture " + path);
            }
            return BLIZZARD_JSON.readValue(json, type);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
