package com.bebis.BeBiS.engine.upgrade;

import com.bebis.BeBiS.engine.power.core.PowerCalculatorEngine;
import com.bebis.BeBiS.engine.upgrade.domain.EquippedItemPair;
import com.bebis.BeBiS.engine.upgrade.domain.UpgradeResult;
import com.bebis.BeBiS.equipment.domain.Equipment;
import com.bebis.BeBiS.equipment.domain.EquippedItem;
import com.bebis.BeBiS.item.EquippableItemBuilder;
import com.bebis.BeBiS.item.domain.Item;
import com.bebis.BeBiS.profile.domain.ClassSpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.collection;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DualSlotUpgradeFinderTest {

    @Mock
    private PowerCalculatorEngine powerCalculator;

    private DualSlotUpgradeFinder finder;

    private final ClassSpec classSpec = ClassSpec.ROGUE_COMBAT;

    @BeforeEach
    void setUp() {
        finder = new DualSlotUpgradeFinder(powerCalculator, new UpgradeResultEvaluator());
    }

    @Test
    void shouldSignalNoItemsAvailableResult_EmptyItemPool() {
        // given / when
        Map<Equipment.Slot, UpgradeResult> result = finder.findUpgrades(getCurrentRingPair(null, null), classSpec, List.of());

        // then
        assertThat(result).containsOnlyKeys(Equipment.Slot.FINGER_1, Equipment.Slot.FINGER_2);
        assertThat(result.values()).hasOnlyElementsOfType(UpgradeResult.NoItemsAvailableResult.class);

        verify(powerCalculator, never()).calculate(any(Item.class), any(ClassSpec.class));
    }

    @Test
    void shouldSignalUpgradeFoundResultAndPutBestItemInBothSlots_NonUniqueEquipped() {
        // given
        Item currentRing1 = EquippableItemBuilder.newRing().withBaseId(1L).build();
        Item currentRing2 = EquippableItemBuilder.newRing().withBaseId(2L).build();

        Item bestRing = EquippableItemBuilder.newRing()
                .withBaseId(3L)
                .withUniqueEquipped(false).build();

        when(powerCalculator.calculate(currentRing1, classSpec)).thenReturn(21.0);
        when(powerCalculator.calculate(currentRing2, classSpec)).thenReturn(37.0);
        when(powerCalculator.calculate(bestRing, classSpec)).thenReturn(69.0);

        List<Item> itemPool = List.of(currentRing1, currentRing2, bestRing);

        // when
        Map<Equipment.Slot, UpgradeResult> result = finder.findUpgrades(getCurrentRingPair(currentRing1, currentRing2), classSpec, itemPool);

        // then
        assertThat(result).containsOnlyKeys(Equipment.Slot.FINGER_1, Equipment.Slot.FINGER_2);

        assertThat(result.values()).asInstanceOf(collection(UpgradeResult.UpgradeFoundResult.class))
                .extracting(UpgradeResult.UpgradeFoundResult::item).containsExactly(bestRing, bestRing);

        verify(powerCalculator, times(itemPool.size())).calculate(argThat(itemPool::contains), eq(classSpec));
    }

    @Test
    void shouldSignalUpgradeFoundResult_UniqueEquippedBestItem_OtherSecondBestItem() {
        // given
        Item currentRing1 = EquippableItemBuilder.newRing().withBaseId(1L).withInventoryType("FINGER").build();
        Item currentRing2 = EquippableItemBuilder.newRing().withBaseId(2L).withInventoryType("FINGER").build();
        Item secondBestRing = EquippableItemBuilder.newRing().withBaseId(3L).withInventoryType("FINGER").build();

        Item bestUniqueRing = EquippableItemBuilder.newRing()
                .withBaseId(4L)
                .withUniqueEquipped(true).build();

        when(powerCalculator.calculate(currentRing1, classSpec)).thenReturn(21.0);
        when(powerCalculator.calculate(currentRing2, classSpec)).thenReturn(37.0);
        when(powerCalculator.calculate(secondBestRing, classSpec)).thenReturn(69.0);
        when(powerCalculator.calculate(bestUniqueRing, classSpec)).thenReturn(100.0);

        List<Item> itemPool = List.of(currentRing1, currentRing2, secondBestRing, bestUniqueRing);

        // when
        Map<Equipment.Slot, UpgradeResult> result = finder.findUpgrades(getCurrentRingPair(currentRing1, currentRing2), classSpec, itemPool);

        // then
        assertThat(result).containsOnlyKeys(Equipment.Slot.FINGER_1, Equipment.Slot.FINGER_2);

        assertThat(result.get(Equipment.Slot.FINGER_1)).isInstanceOfSatisfying(UpgradeResult.UpgradeFoundResult.class,
                res -> assertThat(res.item()).isEqualTo(bestUniqueRing));

        assertThat(result.get(Equipment.Slot.FINGER_2)).isInstanceOfSatisfying(UpgradeResult.UpgradeFoundResult.class,
                res -> assertThat(res.item()).isEqualTo(secondBestRing));

        verify(powerCalculator, times(itemPool.size())).calculate(argThat(itemPool::contains), eq(classSpec));
    }

    @Test
    void shouldSignalUpgradeFoundAndAlreadyBiSResult_UniqueBestItemAndEquippedCurrentRing() {
        // given
        Item uniqueRing = EquippableItemBuilder.newRing()
                .withBaseId(1L)
                .withUniqueEquipped(true).build();
        Item currentRing = EquippableItemBuilder.newRing()
                .withBaseId(2L)
                .withUniqueEquipped(true).build();

        when(powerCalculator.calculate(uniqueRing, classSpec)).thenReturn(69.0);
        when(powerCalculator.calculate(currentRing, classSpec)).thenReturn(21.37);

        List<Item> itemPool = List.of(currentRing, uniqueRing);

        // when
        Map<Equipment.Slot, UpgradeResult> result = finder.findUpgrades(getCurrentRingPair(null, currentRing), classSpec, itemPool);

        // then
        assertThat(result).containsOnlyKeys(Equipment.Slot.FINGER_1, Equipment.Slot.FINGER_2);

        assertThat(result.get(Equipment.Slot.FINGER_1)).isInstanceOfSatisfying(UpgradeResult.UpgradeFoundResult.class,
                res -> assertThat(res.item()).isEqualTo(uniqueRing));

        assertThat(result.get(Equipment.Slot.FINGER_2)).isInstanceOfSatisfying(UpgradeResult.AlreadyBiSResult.class,
                res -> assertThat(res.item()).isEqualTo(currentRing));

        // no equipped items so no additional calculations
        verify(powerCalculator, times(itemPool.size())).calculate(argThat(itemPool::contains), eq(classSpec));
    }

    @Test
    void shouldSignalAlreadyBisResult_BothItemsBiS_DifferentItems() {
        // given
        Item topTrinket1 = EquippableItemBuilder.newRing().withBaseId(1L).withInventoryType("TRINKET").build();
        Item topTrinket2 = EquippableItemBuilder.newRing().withBaseId(2L).withInventoryType("TRINKET").build();
        Item worseTrinket1 = EquippableItemBuilder.newRing().withBaseId(3L).withInventoryType("TRINKET").build();
        Item worseTrinket2 = EquippableItemBuilder.newRing().withBaseId(4L).withInventoryType("TRINKET").build();

        when(powerCalculator.calculate(topTrinket1, classSpec)).thenReturn(69.0);
        when(powerCalculator.calculate(topTrinket2, classSpec)).thenReturn(69.0);
        when(powerCalculator.calculate(worseTrinket1, classSpec)).thenReturn(21.0);
        when(powerCalculator.calculate(worseTrinket2, classSpec)).thenReturn(37.0);

        List<Item> itemPool = List.of(topTrinket1, topTrinket2, worseTrinket1, worseTrinket2);

        // when
        Map<Equipment.Slot, UpgradeResult> result = finder.findUpgrades(getCurrentTrinketPair(topTrinket1, topTrinket2), classSpec, itemPool);

        // then
        assertThat(result).containsOnlyKeys(Equipment.Slot.TRINKET_1, Equipment.Slot.TRINKET_2);

        assertThat(result.get(Equipment.Slot.TRINKET_1)).isInstanceOfSatisfying(UpgradeResult.AlreadyBiSResult.class,
                res -> assertThat(res.item()).isEqualTo(topTrinket1));

        assertThat(result.get(Equipment.Slot.TRINKET_2)).isInstanceOfSatisfying(UpgradeResult.AlreadyBiSResult.class,
                res -> assertThat(res.item()).isEqualTo(topTrinket2));

        verify(powerCalculator, times(itemPool.size())).calculate(argThat(itemPool::contains), eq(classSpec));
    }

    @Test
    void shouldSignalAlreadyBiSAndUpgradeFoundResults_UniqueBiSAndWeakerItemTheOnlyOnesInPool() {
        // given
        Item topUniqueTrinket = EquippableItemBuilder.newRing()
                .withBaseId(1L)
                .withUniqueEquipped(true)
                .build();
        Item otherTrinket = EquippableItemBuilder.newRing().withBaseId(2L).withInventoryType("TRINKET").build();

        when(powerCalculator.calculate(topUniqueTrinket, classSpec)).thenReturn(69.0);
        when(powerCalculator.calculate(otherTrinket, classSpec)).thenReturn(21.37);

        List<Item> itemPool = List.of(topUniqueTrinket, otherTrinket);

        // when
        Map<Equipment.Slot, UpgradeResult> result = finder.findUpgrades(getCurrentTrinketPair(topUniqueTrinket, null), classSpec, itemPool);

        // then
        assertThat(result).containsOnlyKeys(Equipment.Slot.TRINKET_1, Equipment.Slot.TRINKET_2);

        assertThat(result.get(Equipment.Slot.TRINKET_1)).isInstanceOfSatisfying(UpgradeResult.AlreadyBiSResult.class,
                res -> assertThat(res.item()).isEqualTo(topUniqueTrinket));

        assertThat(result.get(Equipment.Slot.TRINKET_2)).isInstanceOfSatisfying(UpgradeResult.UpgradeFoundResult.class,
                res -> assertThat(res.item()).isEqualTo(otherTrinket));

        verify(powerCalculator, times(itemPool.size())).calculate(argThat(itemPool::contains), eq(classSpec));
    }

    @Test
    void shouldSignalUpgradeFoundEvents_BothUpgradesDifferentAndUniqueEquipped() {
        // given
        Item uniqueRing1 = EquippableItemBuilder.newRing().withBaseId(1L)
                .withUniqueEquipped(true)
                .build();
        Item uniqueRing2 = EquippableItemBuilder.newRing().withBaseId(2L)
                .withUniqueEquipped(true)
                .build();
        Item currentRing = EquippableItemBuilder.newRing().withBaseId(3L).build();

        when(powerCalculator.calculate(uniqueRing1, classSpec)).thenReturn(100.0);
        when(powerCalculator.calculate(uniqueRing2, classSpec)).thenReturn(80.0);
        when(powerCalculator.calculate(currentRing, classSpec)).thenReturn(70.0);

        List<Item> itemPool = List.of(uniqueRing1, uniqueRing2, currentRing);

        // when
        Map<Equipment.Slot, UpgradeResult> result = finder.findUpgrades(getCurrentRingPair(currentRing, null), classSpec, itemPool);

        // then
        assertThat(result).containsOnlyKeys(Equipment.Slot.FINGER_1, Equipment.Slot.FINGER_2);

        assertThat(result.get(Equipment.Slot.FINGER_1)).isInstanceOfSatisfying(UpgradeResult.UpgradeFoundResult.class,
                res -> assertThat(res.item()).isEqualTo(uniqueRing2));

        assertThat(result.get(Equipment.Slot.FINGER_2)).isInstanceOfSatisfying(UpgradeResult.UpgradeFoundResult.class,
                res -> assertThat(res.item()).isEqualTo(uniqueRing1));

        verify(powerCalculator, times(itemPool.size())).calculate(argThat(itemPool::contains), eq(classSpec));
    }

    @Test
    void shouldAlwaysAssignItemsToSameSlots_SameTotalScoreOfCandidateItemsConfigurations() {
        // given
        Item ring1 = EquippableItemBuilder.newRing().withBaseId(1L)
                .withInventoryType("FINGER")
                .withUniqueEquipped(true)
                .build();
        Item ring2 = EquippableItemBuilder.newRing().withBaseId(2L).withInventoryType("FINGER").build();

        when(powerCalculator.calculate(ring1, classSpec)).thenReturn(100.0);
        when(powerCalculator.calculate(ring2, classSpec)).thenReturn(80.0);

        List<Item> itemPool = List.of(ring1, ring2);

        // when
        Map<Equipment.Slot, UpgradeResult> result = finder.findUpgrades(getCurrentRingPair(ring1, ring2), classSpec, itemPool);

        // then
        assertThat(result).containsOnlyKeys(Equipment.Slot.FINGER_1, Equipment.Slot.FINGER_2);

        assertThat(result.get(Equipment.Slot.FINGER_1)).isInstanceOfSatisfying(UpgradeResult.AlreadyBiSResult.class,
                res -> assertThat(res.item()).isEqualTo(ring1));

        assertThat(result.get(Equipment.Slot.FINGER_2)).isInstanceOfSatisfying(UpgradeResult.AlreadyBiSResult.class,
                res -> assertThat(res.item()).isEqualTo(ring2));

        verify(powerCalculator, times(itemPool.size())).calculate(argThat(itemPool::contains), eq(classSpec));

    }

    @Test
    void shouldOptimizeCandidateItemsSlotAssignment_ByTotalScoreOfACandidateConfiguration() {
        // given
        Item bestRing = EquippableItemBuilder.newRing().withBaseId(1L)
                .withInventoryType("FINGER")
                .withUniqueEquipped(true)
                .build();
        Item worstRing = EquippableItemBuilder.newRing().withBaseId(2L).withInventoryType("FINGER").build();
        Item otherRing = EquippableItemBuilder.newRing().withBaseId(3L).withInventoryType("FINGER").build();

        when(powerCalculator.calculate(bestRing, classSpec)).thenReturn(80.0);
        when(powerCalculator.calculate(worstRing, classSpec)).thenReturn(10.0);
        when(powerCalculator.calculate(otherRing, classSpec)).thenReturn(50.0);

        List<Item> itemPool = List.of(bestRing, worstRing, otherRing);

        // when
        Map<Equipment.Slot, UpgradeResult> result = finder.findUpgrades(getCurrentRingPair(bestRing, worstRing), classSpec, itemPool);

        // then
        assertThat(result).containsOnlyKeys(Equipment.Slot.FINGER_1, Equipment.Slot.FINGER_2);

        assertThat(result.get(Equipment.Slot.FINGER_1)).isInstanceOfSatisfying(UpgradeResult.AlreadyBiSResult.class,
                res -> assertThat(res.item()).isEqualTo(bestRing));

        assertThat(result.get(Equipment.Slot.FINGER_2)).isInstanceOfSatisfying(UpgradeResult.UpgradeFoundResult.class,
                res -> assertThat(res.item()).isEqualTo(otherRing));

        verify(powerCalculator, times(itemPool.size())).calculate(argThat(itemPool::contains), eq(classSpec));
    }

    @Test
    void shouldSignalUpgradeFoundInOneSlotAndNoItemsInOtherWhenItemPoolHasSingleUniqueEquippedItem() {
        // given
        Item uniqueRing = EquippableItemBuilder.newRing()
                .withBaseId(1L)
                .withUniqueEquipped(true)
                .build();

        when(powerCalculator.calculate(uniqueRing, classSpec)).thenReturn(50.0);

        List<Item> itemPool = List.of(uniqueRing);

        // when
        Map<Equipment.Slot, UpgradeResult> result = finder.findUpgrades(getCurrentRingPair(null, null), classSpec, itemPool);

        // then
        assertThat(result).containsOnlyKeys(Equipment.Slot.FINGER_1, Equipment.Slot.FINGER_2);

        assertThat(result.get(Equipment.Slot.FINGER_1)).isInstanceOfSatisfying(UpgradeResult.UpgradeFoundResult.class,
                res -> assertThat(res.item()).isEqualTo(uniqueRing));

        assertThat(result.get(Equipment.Slot.FINGER_2)).isInstanceOf(UpgradeResult.NoItemsAvailableResult.class);

        verify(powerCalculator, times(itemPool.size())).calculate(argThat(itemPool::contains), eq(classSpec));
    }

    private EquippedItemPair getCurrentRingPair(Item slot1, Item slot2) {
        return new EquippedItemPair(
                new EquippedItemPair.Item(Equipment.Slot.FINGER_1, slot1 != null ? Optional.of(new EquippedItem(slot1, List.of())) : Optional.empty()),
                new EquippedItemPair.Item(Equipment.Slot.FINGER_2, slot2 != null ? Optional.of(new EquippedItem(slot2, List.of())) : Optional.empty()));
    }

    private EquippedItemPair getCurrentTrinketPair(Item slot1, Item slot2) {
        return new EquippedItemPair(
                new EquippedItemPair.Item(Equipment.Slot.TRINKET_1, slot1 != null ? Optional.of(new EquippedItem(slot1, List.of())) : Optional.empty()),
                new EquippedItemPair.Item(Equipment.Slot.TRINKET_2, slot2 != null ? Optional.of(new EquippedItem(slot2, List.of())) : Optional.empty()));
    }

}
