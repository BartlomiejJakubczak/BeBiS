package com.bebis.BeBiS.engine.upgrade;

import com.bebis.BeBiS.engine.power.core.PowerCalculatorEngineProvider;
import com.bebis.BeBiS.engine.upgrade.domain.ItemPool;
import com.bebis.BeBiS.engine.upgrade.domain.ItemScoringContext;
import com.bebis.BeBiS.engine.upgrade.domain.ScoredItem;
import com.bebis.BeBiS.engine.upgrade.domain.ScoredItemPool;
import com.bebis.BeBiS.equipment.domain.Equipment;
import com.bebis.BeBiS.integration.blizzard.BlizzardFixtures;
import com.bebis.BeBiS.item.BenchmarkFixturesConverter;
import com.bebis.BeBiS.item.domain.Item;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.bebis.BeBiS.equipment.domain.Equipment.Slot.*;
import static org.assertj.core.api.Assertions.assertThat;

class BenchmarkTest {

    private static final Set<Equipment.Slot> KNOWN_HITS = Set.of(HEAD, SHOULDER, CHEST, WRIST, FEET);

    private final BenchmarkFixturesConverter converter = new BenchmarkFixturesConverter();
    private final ItemScorer scorer = new ItemScorer(PowerCalculatorEngineProvider.engine());

    @Test
    void combatRogue_preRaid() {
        // given
        BenchmarkReference benchmark = BenchmarkFixtures.loadBenchmark("combat-rogue-preraid.json");

        // when
        TestResult results = evaluate(benchmark);

        // then
        System.out.println(results);

        assertThat(results.hits()).as("known hits").containsAll(KNOWN_HITS);
    }

    private TestResult evaluate(BenchmarkReference benchmark) {
        return new TestResult(scoreSlotContenders(benchmark));
    }

    private List<SlotResult> scoreSlotContenders(BenchmarkReference benchmark) {
        ItemScoringContext context = new ItemScoringContext(benchmark.spec(), benchmark.race());
        return benchmark.slots().entrySet().stream()
                .map(entry -> {
                    Equipment.Slot slot = entry.getKey();

                    ScoredItemPool scoredPool = scorer.scoreItemPool(slot, createFrom(entry.getValue()), context);

                    return new SlotResult(slot, scoredPool, benchmark.bestItemId(slot));
                }).toList();
    }

    private ItemPool createFrom(BenchmarkReference.SlotReference reference) {
        List<Item> pool = reference.candidates().stream()
                .map(BlizzardFixtures::loadBenchmarkItem)
                .map(converter::convert)
                .toList();
        return new ItemPool(pool);
    }

    record SlotResult(Equipment.Slot slot, ScoredItemPool scoredPool, long benchmarkBestItemId) {

        Result result() {
            ScoredItem benchmarkItem = benchmarkItem();
            ScoredItem bestItem = bestRival(benchmarkItem);

            if (benchmarkItem.score() > bestItem.score()) return Result.HIT;
            if (bestItem.score() == benchmarkItem.score()) return Result.TIE;
            return Result.MISS;
        }

        private ScoredItem benchmarkItem() {
            return scoredPool().items().stream()
                    .filter((it) -> it.item().getMetadata().key().baseId() == benchmarkBestItemId()).findFirst()
                    .orElseThrow(() -> new IllegalStateException("Benchmark item with id: " + benchmarkBestItemId() + " was not found in the item pool"));
        }

        private ScoredItem bestRival(ScoredItem benchmarkItem) {
            return scoredPool().items().stream().filter(it -> !it.item().equals(benchmarkItem.item())).findFirst()
                    .orElseThrow(() -> new IllegalStateException("No rivals found for item with id: " + benchmarkItem.item()));
        }
    }

    enum Result {
        HIT, TIE, MISS
    }

    record TestResult(List<SlotResult> results) {

        TestResult {
            results = List.copyOf(results);
        }

        List<SlotResult> allResultingIn(Result result) {
            return results().stream().filter(r -> r.result() == result).toList();
        }

        Set<Equipment.Slot> hits() {
            return allResultingIn(Result.HIT).stream().map(SlotResult::slot).collect(Collectors.toSet());
        }

        /**
         * One aligned row per slot, then one summary line per result. The margin is the benchmark item's score minus
         * its best rival's, so a small positive margin marks a fragile hit.
         */
        @Override
        public String toString() {
            int nameWidth = results.stream()
                    .flatMap(result -> Stream.of(result.benchmarkItem(), result.bestRival(result.benchmarkItem())))
                    .mapToInt(scoredItem -> scoredItem.item().getMetadata().name().length())
                    .max().orElse(0);
            String header = "%-9s %-6s %-" + nameWidth + "s %6s   %-" + nameWidth + "s %6s %7s%n";
            String row = "%-9s %-6s %-" + nameWidth + "s %6.1f   %-" + nameWidth + "s %6.1f %+7.1f%n";

            StringBuilder output = new StringBuilder(System.lineSeparator());
            // Locale.ROOT keeps the decimal point a dot whatever the machine's locale (pl_PL would print 53,5)
            output.append(String.format(Locale.ROOT, header,
                    "SLOT", "RESULT", "BENCHMARK ITEM", "SCORE", "BEST RIVAL", "SCORE", "MARGIN"));
            for (SlotResult result : results) {
                ScoredItem benchmarkItem = result.benchmarkItem();
                ScoredItem bestRival = result.bestRival(benchmarkItem);
                output.append(String.format(Locale.ROOT, row,
                        result.slot(), result.result(),
                        benchmarkItem.item().getMetadata().name(), benchmarkItem.score(),
                        bestRival.item().getMetadata().name(), bestRival.score(),
                        benchmarkItem.score() - bestRival.score()));
            }

            output.append(System.lineSeparator());
            for (Result outcome : Result.values()) {
                List<String> slots = results.stream()
                        .filter(result -> result.result() == outcome)
                        .map(result -> result.slot().name())
                        .toList(); // results keep the reference's slot order, a Set wouldn't
                output.append(String.format(Locale.ROOT, "%-5s %2d  %s%n", outcome, slots.size(), String.join(", ", slots)));
            }
            return output.toString();
        }
    }
}
