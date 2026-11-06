package com.bebis.BeBiS.engine.upgrade;

import com.bebis.BeBiS.engine.upgrade.domain.ScoredItem;
import com.bebis.BeBiS.engine.upgrade.domain.ScoredItemPool;
import com.bebis.BeBiS.item.ArmorBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static com.bebis.BeBiS.equipment.domain.Equipment.Slot.CHEST;
import static org.assertj.core.api.Assertions.assertThat;

class SlotResultTest {

    @ParameterizedTest
    @MethodSource("scoreToResultProvider")
    void shouldClassifyAgainstBestRivalScore(double benchmarkScore, double rivalScore, double competitorScore, BenchmarkTest.Result expectedResult) {
        // given
        long benchmarkItemId = Long.MAX_VALUE;

        ScoredItem benchmark = new ScoredItem(ArmorBuilder.newChest().withBaseId(benchmarkItemId).build(), benchmarkScore);
        ScoredItem rival = new ScoredItem(ArmorBuilder.newChest().withBaseId(2L).build(), rivalScore);
        ScoredItem competitor = new ScoredItem(ArmorBuilder.newChest().withBaseId(3L).build(), competitorScore);

        ScoredItemPool scoredPool = new ScoredItemPool(List.of(benchmark, rival, competitor));

        // when
        BenchmarkTest.SlotResult result = new BenchmarkTest.SlotResult(CHEST, scoredPool, benchmarkItemId);

        // then
        assertThat(result.result()).isEqualTo(expectedResult);
    }

    static Stream<Arguments> scoreToResultProvider() {
        return Stream.of(
                Arguments.of(100.0, 90.0, 70.0, BenchmarkTest.Result.HIT),
                Arguments.of(100.0, 100.0, 70.0, BenchmarkTest.Result.TIE),
                Arguments.of(100.0, 150.0, 70.0, BenchmarkTest.Result.MISS)
        );
    }

    @Test
    void shouldClassifyAsTie_WhenBenchmarkItemWinsOnlyTheIdTieBreak() {
        // given
        long benchmarkItemId = 1L;
        double bestScore = 100.0;

        ScoredItem benchmark = new ScoredItem(ArmorBuilder.newChest().withBaseId(benchmarkItemId).build(), bestScore);
        ScoredItem rival = new ScoredItem(ArmorBuilder.newChest().withBaseId(2L).build(), bestScore);
        ScoredItem competitor = new ScoredItem(ArmorBuilder.newChest().withBaseId(3L).build(), 0.0);

        ScoredItemPool scoredPool = new ScoredItemPool(List.of(benchmark, rival, competitor));

        // when
        BenchmarkTest.SlotResult result = new BenchmarkTest.SlotResult(CHEST, scoredPool, benchmarkItemId);

        // then
        assertThat(result.result()).isEqualTo(BenchmarkTest.Result.TIE);
        // would be a fake HIT if id determinism of ScoredItemPool wasn't dealt with in result() method of SlotResult
    }
}
