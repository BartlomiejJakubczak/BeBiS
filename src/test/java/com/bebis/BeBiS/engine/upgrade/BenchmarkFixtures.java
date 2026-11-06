package com.bebis.BeBiS.engine.upgrade;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/**
 * Strict fixture loader class. It's meant to fail on wrongly structured JSON benchmark fixtures located under resources/benchmark/,
 * as opposed to fixtures coming from Blizzard directly.
 */
public final class BenchmarkFixtures {

    private static final ObjectMapper BENCHMARK_JSON = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

    private BenchmarkFixtures() {
    }

    public static BenchmarkReference loadBenchmark(String fileName) {
        return load("/benchmark/" + fileName, BenchmarkReference.class);
    }

    private static <T> T load(String path, Class<T> type) {
        try (InputStream json = BenchmarkFixtures.class.getResourceAsStream(path)) {
            if (json == null) {
                throw new IllegalArgumentException("Missing fixture " + path);
            }
            return BENCHMARK_JSON.readValue(json, type);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
