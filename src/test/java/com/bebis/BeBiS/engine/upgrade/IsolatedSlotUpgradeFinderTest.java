package com.bebis.BeBiS.engine.upgrade;

import com.bebis.BeBiS.engine.upgrade.domain.ItemPool;
import com.bebis.BeBiS.engine.upgrade.domain.ScoredItem;
import com.bebis.BeBiS.engine.upgrade.domain.ScoredItemPool;
import com.bebis.BeBiS.engine.upgrade.domain.UpgradeResult;
import com.bebis.BeBiS.equipment.domain.Equipment;
import com.bebis.BeBiS.equipment.domain.EquippedItem;
import com.bebis.BeBiS.item.ArmorBuilder;
import com.bebis.BeBiS.item.WeaponBuilder;
import com.bebis.BeBiS.item.domain.Item;
import com.bebis.BeBiS.profile.domain.ClassSpec;
import com.bebis.BeBiS.profile.domain.WowCharacter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static com.bebis.BeBiS.engine.upgrade.IsolatedSlotUpgradeOrchestrator.SlotUpgradeQuery;
import static com.bebis.BeBiS.equipment.domain.Equipment.Slot.CHEST;
import static com.bebis.BeBiS.equipment.domain.Equipment.Slot.RANGED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class IsolatedSlotUpgradeFinderTest {

    @Mock
    private ItemScorer scorer;

    private IsolatedSlotUpgradeFinder finder;

    // test data
    private final ClassSpec classSpec = ClassSpec.ROGUE_COMBAT;
    private final WowCharacter.Race race = WowCharacter.Race.HUMAN;

    @BeforeEach
    void setup() {
        finder = new IsolatedSlotUpgradeFinder(scorer, new UpgradeResultEvaluator());
    }

    @Test
    void shouldSignalUpgradeFoundResult_EmptyEquippedItem() {
        // given
        Item worseChest = ArmorBuilder.newChest().withBaseId(1L).build();
        Item betterChest = ArmorBuilder.newChest().withBaseId(2L).build();

        ItemPool itemPool = givenPoolScoredAs(CHEST, Map.of(worseChest, 21.0, betterChest, 37.0));

        // when
        UpgradeResult result = finder.findUpgrade(new SlotUpgradeQuery(CHEST, Optional.empty(), itemPool), classSpec, race);

        // then
        assertThat(result).asInstanceOf(type(UpgradeResult.UpgradeFoundResult.class))
                .extracting(UpgradeResult.UpgradeFoundResult::item).isEqualTo(betterChest);
    }

    @Test
    void shouldSignalNoItemsAvailableResult_EmptyItemPool() {
        // given
        ItemPool itemPool = givenPoolScoredAs(CHEST, Map.of());

        // when
        UpgradeResult result = finder.findUpgrade(new SlotUpgradeQuery(CHEST, Optional.empty(), itemPool), classSpec, race);

        // then
        assertThat(result).isInstanceOf(UpgradeResult.NoItemsAvailableResult.class);
    }

    @Test
    void shouldSignalUpgradeFoundResult_EquippedItemWorseThanBestItemInPool() {
        // given
        Item currentChest = ArmorBuilder.newChest().withBaseId(1L).build();
        Item otherChest = ArmorBuilder.newChest().withBaseId(2L).build();
        Item bestChest = ArmorBuilder.newChest().withBaseId(3L).build();

        ItemPool itemPool = givenPoolScoredAs(CHEST, Map.of(currentChest, 21.0, otherChest, 37.0, bestChest, 69.0));

        // when
        UpgradeResult result = finder.findUpgrade(new SlotUpgradeQuery(CHEST, equipped(currentChest), itemPool), classSpec, race);

        // then
        assertThat(result).asInstanceOf(type(UpgradeResult.UpgradeFoundResult.class))
                .extracting(UpgradeResult.UpgradeFoundResult::item).isEqualTo(bestChest);
    }

    @ParameterizedTest
    @MethodSource("scoreProvider")
    void shouldSignalAlreadyBiSResult_EquippedItemIsBiSOrEqualToAnotherItemInPool(double eqItemScore, double otherItemScore) {
        // given
        Item currentChest = ArmorBuilder.newChest().withBaseId(1L).build();
        Item otherChest = ArmorBuilder.newChest().withBaseId(2L).build();

        ItemPool itemPool = givenPoolScoredAs(CHEST, Map.of(currentChest, eqItemScore, otherChest, otherItemScore));

        // when
        UpgradeResult result = finder.findUpgrade(new SlotUpgradeQuery(CHEST, equipped(currentChest), itemPool), classSpec, race);

        // then
        assertThat(result).asInstanceOf(type(UpgradeResult.AlreadyBiSResult.class))
                .extracting(UpgradeResult.AlreadyBiSResult::item).isEqualTo(currentChest);
    }

    private static Stream<Arguments> scoreProvider() {
        return Stream.of(
                Arguments.of(37.0, 21.0),
                Arguments.of(21.0, 21.0)
        );
    }

    /**
     * Regression: the ranged slot used to be scored without its slot, so weapon DPS and ranged racials were ignored.
     * The stub only answers for {@code RANGED}, so scoring the pool for any other slot fails the test.
     */
    @Test
    void shouldScorePoolForTheQueriedSlot_RangedWeapons() {
        // given
        Item currentBow = WeaponBuilder.newRanged().withBaseId(1L).build();
        Item betterBow = WeaponBuilder.newRanged().withBaseId(2L).build();

        ItemPool itemPool = givenPoolScoredAs(RANGED, Map.of(currentBow, 30.0, betterBow, 45.0));

        // when
        UpgradeResult result = finder.findUpgrade(new SlotUpgradeQuery(RANGED, equipped(currentBow), itemPool), classSpec, race);

        // then
        assertThat(result).asInstanceOf(type(UpgradeResult.UpgradeFoundResult.class))
                .extracting(UpgradeResult.UpgradeFoundResult::item).isEqualTo(betterBow);
    }

    /**
     * Stubs the scorer to score the returned pool with the given scores, as {@link ItemScorer} would for this slot.
     */
    private ItemPool givenPoolScoredAs(Equipment.Slot slot, Map<Item, Double> scores) {
        ItemPool itemPool = new ItemPool(List.copyOf(scores.keySet()));
        List<ScoredItem> scoredItems = scores.entrySet().stream()
                .map(entry -> new ScoredItem(entry.getKey(), entry.getValue()))
                .toList();
        when(scorer.scoreItemPool(slot, itemPool, classSpec, race)).thenReturn(new ScoredItemPool(scoredItems));
        return itemPool;
    }

    private Optional<EquippedItem> equipped(Item item) {
        return Optional.of(new EquippedItem(item, List.of()));
    }
}
