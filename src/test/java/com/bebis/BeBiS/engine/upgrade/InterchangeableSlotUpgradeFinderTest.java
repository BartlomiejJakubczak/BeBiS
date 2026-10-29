package com.bebis.BeBiS.engine.upgrade;

import com.bebis.BeBiS.engine.upgrade.domain.EquippedItemPair;
import com.bebis.BeBiS.engine.upgrade.domain.InterchangeableSlots;
import com.bebis.BeBiS.engine.upgrade.domain.ItemPool;
import com.bebis.BeBiS.engine.upgrade.domain.ItemScoringContext;
import com.bebis.BeBiS.engine.upgrade.domain.ScoredItem;
import com.bebis.BeBiS.engine.upgrade.domain.ScoredItemPool;
import com.bebis.BeBiS.engine.upgrade.domain.UpgradeResult;
import com.bebis.BeBiS.equipment.domain.Equipment;
import com.bebis.BeBiS.equipment.domain.EquippedItem;
import com.bebis.BeBiS.item.EquippableItemBuilder;
import com.bebis.BeBiS.item.domain.Item;
import com.bebis.BeBiS.item.domain.QuestSource;
import com.bebis.BeBiS.item.domain.RewardKind;
import com.bebis.BeBiS.profile.domain.ClassSpec;
import com.bebis.BeBiS.profile.domain.WowCharacter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.bebis.BeBiS.engine.upgrade.InterchangeableSlotsUpgradeOrchestrator.SlotUpgradeQuery;
import static com.bebis.BeBiS.equipment.domain.Equipment.Slot.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.collection;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class InterchangeableSlotUpgradeFinderTest {
    @Mock
    private ItemScorer scorer;

    private InterchangeableSlotUpgradeFinder finder;

    private final ClassSpec classSpec = ClassSpec.ROGUE_COMBAT;
    private final WowCharacter.Race race = WowCharacter.Race.HUMAN;
    private final ItemScoringContext context = new ItemScoringContext(classSpec, race);

    @BeforeEach
    void setUp() {
        finder = new InterchangeableSlotUpgradeFinder(scorer, new UpgradeResultEvaluator());
    }

    @Test
    void shouldSignalNoItemsAvailableResult_EmptyItemPool() {
        // given
        ItemPool itemPool = givenPoolScoredAs(InterchangeableSlots.FINGERS, Map.of());

        // when
        Map<Equipment.Slot, UpgradeResult> result = finder.findUpgrades(query(InterchangeableSlots.FINGERS, null, null, itemPool), context);

        // then
        assertThat(result).containsOnlyKeys(FINGER_1, FINGER_2);
        assertThat(result.values()).hasOnlyElementsOfType(UpgradeResult.NoItemsAvailableResult.class);
    }

    @Test
    void shouldSignalUpgradeFoundResultAndPutBestItemInBothSlots_NonUniqueEquipped() {
        // given
        Item currentRing1 = EquippableItemBuilder.newRing().withBaseId(1L).build();
        Item currentRing2 = EquippableItemBuilder.newRing().withBaseId(2L).build();
        Item bestRing = EquippableItemBuilder.newRing().withBaseId(3L).withUniqueEquipped(false).build();

        ItemPool itemPool = givenPoolScoredAs(InterchangeableSlots.FINGERS, Map.of(currentRing1, 21.0, currentRing2, 37.0, bestRing, 69.0));

        // when
        Map<Equipment.Slot, UpgradeResult> result = finder.findUpgrades(query(InterchangeableSlots.FINGERS, currentRing1, currentRing2, itemPool), context);

        // then
        assertThat(result).containsOnlyKeys(FINGER_1, FINGER_2);
        assertThat(result.values()).asInstanceOf(collection(UpgradeResult.UpgradeFoundResult.class))
                .extracting(UpgradeResult.UpgradeFoundResult::item).containsExactly(bestRing, bestRing);
    }

    @Test
    void shouldSignalUpgradeFoundResult_UniqueEquippedBestItem_OtherSecondBestItem() {
        // given
        Item currentRing1 = EquippableItemBuilder.newRing().withBaseId(1L).build();
        Item currentRing2 = EquippableItemBuilder.newRing().withBaseId(2L).build();
        Item secondBestRing = EquippableItemBuilder.newRing().withBaseId(3L).build();
        Item bestUniqueRing = EquippableItemBuilder.newRing().withBaseId(4L).withUniqueEquipped(true).build();

        ItemPool itemPool = givenPoolScoredAs(InterchangeableSlots.FINGERS,
                Map.of(currentRing1, 21.0, currentRing2, 37.0, secondBestRing, 69.0, bestUniqueRing, 100.0));

        // when
        Map<Equipment.Slot, UpgradeResult> result = finder.findUpgrades(query(InterchangeableSlots.FINGERS, currentRing1, currentRing2, itemPool), context);

        // then
        assertThat(result).containsOnlyKeys(FINGER_1, FINGER_2);
        assertThat(result.get(FINGER_1)).isInstanceOfSatisfying(UpgradeResult.UpgradeFoundResult.class,
                res -> assertThat(res.item()).isEqualTo(bestUniqueRing));
        assertThat(result.get(FINGER_2)).isInstanceOfSatisfying(UpgradeResult.UpgradeFoundResult.class,
                res -> assertThat(res.item()).isEqualTo(secondBestRing));
    }

    @Test
    void shouldSignalUpgradeFoundResult_QuestRewardBestItem_OtherSecondBestItem() {
        // given (Dragonslayer's Signet: a quest reward that isn't Unique, so only the quest source limits it)
        Item currentRing = EquippableItemBuilder.newRing().withBaseId(1L).build();
        Item secondBestRing = EquippableItemBuilder.newRing().withBaseId(2L).build();
        Item questRewardRing = EquippableItemBuilder.newRing().withBaseId(18403L).withName("Dragonslayer's Signet")
                .withUniqueEquipped(false)
                .withSources(new QuestSource(7491L, 60, 60, RewardKind.CHOICE, Set.of(), Set.of()))
                .build();

        ItemPool itemPool = givenPoolScoredAs(InterchangeableSlots.FINGERS, Map.of(currentRing, 21.0, secondBestRing, 69.0, questRewardRing, 100.0));

        // when
        Map<Equipment.Slot, UpgradeResult> result = finder.findUpgrades(query(InterchangeableSlots.FINGERS, currentRing, null, itemPool), context);

        // then
        assertThat(result.get(FINGER_1)).isInstanceOfSatisfying(UpgradeResult.UpgradeFoundResult.class,
                res -> assertThat(res.item()).isEqualTo(questRewardRing));
        assertThat(result.get(FINGER_2)).isInstanceOfSatisfying(UpgradeResult.UpgradeFoundResult.class,
                res -> assertThat(res.item()).isEqualTo(secondBestRing));
    }

    @Test
    void shouldSignalUpgradeFoundAndAlreadyBiSResult_UniqueBestItemAndEquippedCurrentRing() {
        // given
        Item uniqueRing = EquippableItemBuilder.newRing().withBaseId(1L).withUniqueEquipped(true).build();
        Item currentRing = EquippableItemBuilder.newRing().withBaseId(2L).withUniqueEquipped(true).build();

        ItemPool itemPool = givenPoolScoredAs(InterchangeableSlots.FINGERS, Map.of(currentRing, 21.37, uniqueRing, 69.0));

        // when
        Map<Equipment.Slot, UpgradeResult> result = finder.findUpgrades(query(InterchangeableSlots.FINGERS, null, currentRing, itemPool), context);

        // then
        assertThat(result).containsOnlyKeys(FINGER_1, FINGER_2);
        assertThat(result.get(FINGER_1)).isInstanceOfSatisfying(UpgradeResult.UpgradeFoundResult.class,
                res -> assertThat(res.item()).isEqualTo(uniqueRing));
        assertThat(result.get(FINGER_2)).isInstanceOfSatisfying(UpgradeResult.AlreadyBiSResult.class,
                res -> assertThat(res.item()).isEqualTo(currentRing));
    }

    @Test
    void shouldSignalAlreadyBisResult_BothItemsBiS_DifferentItems() {
        // given
        Item topTrinket1 = EquippableItemBuilder.newRing().withBaseId(1L).withInventoryType("TRINKET").build();
        Item topTrinket2 = EquippableItemBuilder.newRing().withBaseId(2L).withInventoryType("TRINKET").build();
        Item worseTrinket1 = EquippableItemBuilder.newRing().withBaseId(3L).withInventoryType("TRINKET").build();
        Item worseTrinket2 = EquippableItemBuilder.newRing().withBaseId(4L).withInventoryType("TRINKET").build();

        ItemPool itemPool = givenPoolScoredAs(InterchangeableSlots.TRINKETS,
                Map.of(topTrinket1, 69.0, topTrinket2, 69.0, worseTrinket1, 21.0, worseTrinket2, 37.0));

        // when
        Map<Equipment.Slot, UpgradeResult> result = finder.findUpgrades(query(InterchangeableSlots.TRINKETS, topTrinket1, topTrinket2, itemPool), context);

        // then
        assertThat(result).containsOnlyKeys(TRINKET_1, TRINKET_2);
        assertThat(result.get(TRINKET_1)).isInstanceOfSatisfying(UpgradeResult.AlreadyBiSResult.class,
                res -> assertThat(res.item()).isEqualTo(topTrinket1));
        assertThat(result.get(TRINKET_2)).isInstanceOfSatisfying(UpgradeResult.AlreadyBiSResult.class,
                res -> assertThat(res.item()).isEqualTo(topTrinket2));
    }

    @Test
    void shouldSignalAlreadyBiSAndUpgradeFoundResults_UniqueBiSAndWeakerItemTheOnlyOnesInPool() {
        // given
        Item topUniqueTrinket = EquippableItemBuilder.newRing().withBaseId(1L).withInventoryType("TRINKET")
                .withUniqueEquipped(true).build();
        Item otherTrinket = EquippableItemBuilder.newRing().withBaseId(2L).withInventoryType("TRINKET").build();

        ItemPool itemPool = givenPoolScoredAs(InterchangeableSlots.TRINKETS, Map.of(topUniqueTrinket, 69.0, otherTrinket, 21.37));

        // when
        Map<Equipment.Slot, UpgradeResult> result = finder.findUpgrades(query(InterchangeableSlots.TRINKETS, topUniqueTrinket, null, itemPool), context);

        // then
        assertThat(result).containsOnlyKeys(TRINKET_1, TRINKET_2);
        assertThat(result.get(TRINKET_1)).isInstanceOfSatisfying(UpgradeResult.AlreadyBiSResult.class,
                res -> assertThat(res.item()).isEqualTo(topUniqueTrinket));
        assertThat(result.get(TRINKET_2)).isInstanceOfSatisfying(UpgradeResult.UpgradeFoundResult.class,
                res -> assertThat(res.item()).isEqualTo(otherTrinket));
    }

    @Test
    void shouldSignalUpgradeFoundEvents_BothUpgradesDifferentAndUniqueEquipped() {
        // given
        Item uniqueRing1 = EquippableItemBuilder.newRing().withBaseId(1L).withUniqueEquipped(true).build();
        Item uniqueRing2 = EquippableItemBuilder.newRing().withBaseId(2L).withUniqueEquipped(true).build();
        Item currentRing = EquippableItemBuilder.newRing().withBaseId(3L).build();

        ItemPool itemPool = givenPoolScoredAs(InterchangeableSlots.FINGERS, Map.of(uniqueRing1, 100.0, uniqueRing2, 80.0, currentRing, 70.0));

        // when
        Map<Equipment.Slot, UpgradeResult> result = finder.findUpgrades(query(InterchangeableSlots.FINGERS, currentRing, null, itemPool), context);

        // then
        assertThat(result).containsOnlyKeys(FINGER_1, FINGER_2);
        assertThat(result.get(FINGER_1)).isInstanceOfSatisfying(UpgradeResult.UpgradeFoundResult.class,
                res -> assertThat(res.item()).isEqualTo(uniqueRing2));
        assertThat(result.get(FINGER_2)).isInstanceOfSatisfying(UpgradeResult.UpgradeFoundResult.class,
                res -> assertThat(res.item()).isEqualTo(uniqueRing1));
    }

    @Test
    void shouldAlwaysAssignItemsToSameSlots_SameTotalScoreOfCandidateItemsConfigurations() {
        // given
        Item ring1 = EquippableItemBuilder.newRing().withBaseId(1L).withUniqueEquipped(true).build();
        Item ring2 = EquippableItemBuilder.newRing().withBaseId(2L).build();

        ItemPool itemPool = givenPoolScoredAs(InterchangeableSlots.FINGERS, Map.of(ring1, 100.0, ring2, 80.0));

        // when
        Map<Equipment.Slot, UpgradeResult> result = finder.findUpgrades(query(InterchangeableSlots.FINGERS, ring1, ring2, itemPool), context);

        // then
        assertThat(result).containsOnlyKeys(FINGER_1, FINGER_2);
        assertThat(result.get(FINGER_1)).isInstanceOfSatisfying(UpgradeResult.AlreadyBiSResult.class,
                res -> assertThat(res.item()).isEqualTo(ring1));
        assertThat(result.get(FINGER_2)).isInstanceOfSatisfying(UpgradeResult.AlreadyBiSResult.class,
                res -> assertThat(res.item()).isEqualTo(ring2));
    }

    @Test
    void shouldOptimizeCandidateItemsSlotAssignment_ByTotalScoreOfACandidateConfiguration() {
        // given
        Item bestRing = EquippableItemBuilder.newRing().withBaseId(1L).withUniqueEquipped(true).build();
        Item worstRing = EquippableItemBuilder.newRing().withBaseId(2L).build();
        Item otherRing = EquippableItemBuilder.newRing().withBaseId(3L).build();

        ItemPool itemPool = givenPoolScoredAs(InterchangeableSlots.FINGERS, Map.of(bestRing, 80.0, worstRing, 10.0, otherRing, 50.0));

        // when
        Map<Equipment.Slot, UpgradeResult> result = finder.findUpgrades(query(InterchangeableSlots.FINGERS, bestRing, worstRing, itemPool), context);

        // then
        assertThat(result).containsOnlyKeys(FINGER_1, FINGER_2);
        assertThat(result.get(FINGER_1)).isInstanceOfSatisfying(UpgradeResult.AlreadyBiSResult.class,
                res -> assertThat(res.item()).isEqualTo(bestRing));
        assertThat(result.get(FINGER_2)).isInstanceOfSatisfying(UpgradeResult.UpgradeFoundResult.class,
                res -> assertThat(res.item()).isEqualTo(otherRing));
    }

    @Test
    void shouldSignalUpgradeFoundInOneSlotAndNoItemsInOther_PoolHasSingleUniqueItem() {
        // given
        Item uniqueRing = EquippableItemBuilder.newRing().withBaseId(1L).withUniqueEquipped(true).build();

        ItemPool itemPool = givenPoolScoredAs(InterchangeableSlots.FINGERS, Map.of(uniqueRing, 50.0));

        // when
        Map<Equipment.Slot, UpgradeResult> result = finder.findUpgrades(query(InterchangeableSlots.FINGERS, null, null, itemPool), context);

        // then
        assertThat(result).containsOnlyKeys(FINGER_1, FINGER_2);
        assertThat(result.get(FINGER_1)).isInstanceOfSatisfying(UpgradeResult.UpgradeFoundResult.class,
                res -> assertThat(res.item()).isEqualTo(uniqueRing));
        assertThat(result.get(FINGER_2)).isInstanceOf(UpgradeResult.NoItemsAvailableResult.class);
    }

    @Test
    void shouldSignalUpgradeFoundInOneSlotAndNoItemsInOther_PoolHasSingleQuestReward() {
        // given
        Item questRewardRing = EquippableItemBuilder.newRing().withBaseId(18403L).withName("Dragonslayer's Signet")
                .withUniqueEquipped(false)
                .withSources(new QuestSource(7491L, 60, 60, RewardKind.CHOICE, Set.of(), Set.of()))
                .build();

        ItemPool itemPool = givenPoolScoredAs(InterchangeableSlots.FINGERS, Map.of(questRewardRing, 50.0));

        // when
        Map<Equipment.Slot, UpgradeResult> result = finder.findUpgrades(query(InterchangeableSlots.FINGERS, null, null, itemPool), context);

        // then
        assertThat(result.get(FINGER_1)).isInstanceOfSatisfying(UpgradeResult.UpgradeFoundResult.class,
                res -> assertThat(res.item()).isEqualTo(questRewardRing));
        assertThat(result.get(FINGER_2)).isInstanceOf(UpgradeResult.NoItemsAvailableResult.class);
    }

    /**
     * Stubs the scorer to score the returned pool with the given scores. Both slots share one pool, scored once for
     * the slot the pair picks for scoring.
     */
    private ItemPool givenPoolScoredAs(InterchangeableSlots slots, Map<Item, Double> scores) {
        ItemPool itemPool = new ItemPool(List.copyOf(scores.keySet()));
        List<ScoredItem> scoredItems = scores.entrySet().stream()
                .map(entry -> new ScoredItem(entry.getKey(), entry.getValue()))
                .toList();
        when(scorer.scoreItemPool(slots.pickSlotForScoring(), itemPool, context)).thenReturn(new ScoredItemPool(scoredItems));
        return itemPool;
    }

    private SlotUpgradeQuery query(InterchangeableSlots slots, Item equipped1, Item equipped2, ItemPool itemPool) {
        return new SlotUpgradeQuery(slots, new EquippedItemPair(equipped(equipped1), equipped(equipped2)), itemPool);
    }

    private Optional<EquippedItem> equipped(Item item) {
        return Optional.ofNullable(item).map(it -> new EquippedItem(it, List.of()));
    }
}
