package com.bebis.BeBiS.engine.upgrade;

import com.bebis.BeBiS.engine.power.core.PowerCalculatorEngine;
import com.bebis.BeBiS.engine.upgrade.DualSlotUpgradeFinder.EquippedItemPair;
import com.bebis.BeBiS.engine.upgrade.domain.UpgradeResult;
import com.bebis.BeBiS.equipment.domain.Equipment;
import com.bebis.BeBiS.equipment.domain.EquippedItem;
import com.bebis.BeBiS.item.ItemBuilder;
import com.bebis.BeBiS.item.domain.Item;
import com.bebis.BeBiS.profile.domain.ClassSpec;
import com.bebis.BeBiS.profile.domain.WowCharacter;
import com.bebis.BeBiS.profile.domain.WowCharacterBuilder;
import com.bebis.BeBiS.profile.domain.WowTalents;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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

    @Captor
    private ArgumentCaptor<Item> calculationCaptor;

    // test data
    private final WowCharacter wowCharacter = WowCharacterBuilder.newWowCharacter().build();

    private final Optional<WowTalents> talents = Optional.of(getTalents());

    private final ClassSpec classSpec = ClassSpec.getClassSpec(wowCharacter.wowClass(), talents);

    @BeforeEach
    void setUp() {
        finder = new DualSlotUpgradeFinder(powerCalculator, new UpgradeResultEvaluator());
    }

    @Test
    void shouldSignalNoItemsAvailableResult_EmptyItemPool() {
        // given
        // when
        Map<Equipment.Slot, UpgradeResult> result = finder.findUpgrades(getCurrentRingPair(Optional.empty(), Optional.empty()), wowCharacter, talents, List.of());

        // then
        assertThat(result).containsOnlyKeys(Equipment.Slot.FINGER_1, Equipment.Slot.FINGER_2);
        assertThat(result.values()).hasOnlyElementsOfType(UpgradeResult.NoItemsAvailableResult.class);

        verify(powerCalculator, never()).calculate(any(Item.class), any(ClassSpec.class), any(WowCharacter.Race.class));
    }

    @Test
    void shouldSignalUpgradeFoundResultAndPutBestItemInBothSlots_NonUniqueEquipped() {
        // given
        Item currentRing1 = ItemBuilder.newEquippableItem().withBaseId(1L).withInventoryType("FINGER").build();
        Item currentRing2 = ItemBuilder.newEquippableItem().withBaseId(2L).withInventoryType("FINGER").build();

        Item bestRing = ItemBuilder.newEquippableItem()
                .withBaseId(3L)
                .withInventoryType("FINGER")
                .withUniqueEquipped(false).build();

        when(powerCalculator.calculate(currentRing1, classSpec, wowCharacter.race())).thenReturn(21.0);
        when(powerCalculator.calculate(currentRing2, classSpec, wowCharacter.race())).thenReturn(37.0);
        when(powerCalculator.calculate(bestRing, classSpec, wowCharacter.race())).thenReturn(69.0);

        EquippedItemPair equippedItemPair = getCurrentRingPair(Optional.of(new EquippedItem(currentRing1, List.of())), Optional.of(new EquippedItem(currentRing2, List.of())));
        List<Item> itemPool = List.of(currentRing1, currentRing2, bestRing);

        // when
        Map<Equipment.Slot, UpgradeResult> result = finder.findUpgrades(equippedItemPair, wowCharacter, talents, itemPool);

        // then
        assertThat(result).containsOnlyKeys(Equipment.Slot.FINGER_1, Equipment.Slot.FINGER_2);

        assertThat(result.values()).asInstanceOf(collection(UpgradeResult.UpgradeFoundResult.class))
                .extracting(UpgradeResult.UpgradeFoundResult::item).containsExactly(bestRing, bestRing);

        verify(powerCalculator, times(itemPool.size()))
                .calculate(argThat(itemPool::contains), eq(classSpec), eq(wowCharacter.race()));
    }

    @Test
    void shouldSignalUpgradeFoundResult_UniqueEquippedBestItem_OtherSecondBestItem() {
        // given
        Item currentRing1 = ItemBuilder.newEquippableItem().withBaseId(1L).withInventoryType("FINGER").build();
        Item currentRing2 = ItemBuilder.newEquippableItem().withBaseId(2L).withInventoryType("FINGER").build();
        Item secondBestRing = ItemBuilder.newEquippableItem().withBaseId(3L).withInventoryType("FINGER").build();

        Item bestUniqueRing = ItemBuilder.newEquippableItem()
                .withBaseId(4L)
                .withInventoryType("FINGER")
                .withUniqueEquipped(true).build();

        when(powerCalculator.calculate(currentRing1, classSpec, wowCharacter.race())).thenReturn(21.0);
        when(powerCalculator.calculate(currentRing2, classSpec, wowCharacter.race())).thenReturn(37.0);
        when(powerCalculator.calculate(secondBestRing, classSpec, wowCharacter.race())).thenReturn(69.0);
        when(powerCalculator.calculate(bestUniqueRing, classSpec, wowCharacter.race())).thenReturn(100.0);

        EquippedItemPair equippedItemPair = getCurrentRingPair(Optional.of(new EquippedItem(currentRing1, List.of())), Optional.of(new EquippedItem(currentRing2, List.of())));
        List<Item> itemPool = List.of(currentRing1, currentRing2, secondBestRing, bestUniqueRing);

        // when
        Map<Equipment.Slot, UpgradeResult> result = finder.findUpgrades(equippedItemPair, wowCharacter, talents, itemPool);

        // then
        assertThat(result).containsOnlyKeys(Equipment.Slot.FINGER_1, Equipment.Slot.FINGER_2);

        assertThat(result.get(Equipment.Slot.FINGER_1)).isInstanceOfSatisfying(UpgradeResult.UpgradeFoundResult.class,
                res -> assertThat(res.item()).isEqualTo(bestUniqueRing));

        assertThat(result.get(Equipment.Slot.FINGER_2)).isInstanceOfSatisfying(UpgradeResult.UpgradeFoundResult.class,
                res -> assertThat(res.item()).isEqualTo(secondBestRing));

        verify(powerCalculator, times(itemPool.size()))
                .calculate(argThat(itemPool::contains), eq(classSpec), eq(wowCharacter.race()));
    }

    @Test
    void shouldSignalUpgradeFoundResult_UniqueItemTheOnlyItemInTheItemPool() {
        // given
        Item uniqueRing = ItemBuilder.newEquippableItem()
                .withBaseId(1L)
                .withInventoryType("FINGER")
                .withUniqueEquipped(true).build();

        when(powerCalculator.calculate(uniqueRing, classSpec, wowCharacter.race())).thenReturn(69.0);

        List<Item> itemPool = List.of(uniqueRing);

        // when
        Map<Equipment.Slot, UpgradeResult> result = finder.findUpgrades(getCurrentRingPair(Optional.empty(), Optional.empty()), wowCharacter, talents, itemPool);

        // then
        assertThat(result).containsOnlyKeys(Equipment.Slot.FINGER_1, Equipment.Slot.FINGER_2);

        assertThat(result.get(Equipment.Slot.FINGER_1)).isInstanceOfSatisfying(UpgradeResult.UpgradeFoundResult.class,
                res -> assertThat(res.item()).isEqualTo(uniqueRing));

        assertThat(result.get(Equipment.Slot.FINGER_2)).isInstanceOf(UpgradeResult.NoItemsAvailableResult.class);

        // no equipped items so no additional calculations
        verify(powerCalculator, times(itemPool.size()))
                .calculate(argThat(itemPool::contains), eq(classSpec), eq(wowCharacter.race()));
    }

    @Test
    void shouldSignalAlreadyBisResult_BothItemsBiS_DifferentItems() {
        // given
        Item topTrinket1 = ItemBuilder.newEquippableItem().withBaseId(1L).withInventoryType("TRINKET").build();
        Item topTrinket2 = ItemBuilder.newEquippableItem().withBaseId(2L).withInventoryType("TRINKET").build();
        Item worseTrinket1 = ItemBuilder.newEquippableItem().withBaseId(3L).withInventoryType("TRINKET").build();
        Item worseTrinket2 = ItemBuilder.newEquippableItem().withBaseId(4L).withInventoryType("TRINKET").build();

        when(powerCalculator.calculate(topTrinket1, classSpec, wowCharacter.race())).thenReturn(69.0);
        when(powerCalculator.calculate(topTrinket2, classSpec, wowCharacter.race())).thenReturn(69.0);
        when(powerCalculator.calculate(worseTrinket1, classSpec, wowCharacter.race())).thenReturn(21.0);
        when(powerCalculator.calculate(worseTrinket2, classSpec, wowCharacter.race())).thenReturn(37.0);

        List<Item> itemPool = List.of(topTrinket1, topTrinket2, worseTrinket1, worseTrinket2);

        // when
        Map<Equipment.Slot, UpgradeResult> result = finder.findUpgrades(getCurrentTrinketPair(Optional.of(new EquippedItem(topTrinket1, List.of())),
                Optional.of(new EquippedItem(topTrinket2, List.of()))), wowCharacter, talents, itemPool);

        // then
        assertThat(result).containsOnlyKeys(Equipment.Slot.TRINKET_1, Equipment.Slot.TRINKET_2);

        assertThat(result.get(Equipment.Slot.TRINKET_1)).isInstanceOfSatisfying(UpgradeResult.AlreadyBiSResult.class,
                res -> assertThat(res.item()).isEqualTo(topTrinket1));

        assertThat(result.get(Equipment.Slot.TRINKET_2)).isInstanceOfSatisfying(UpgradeResult.AlreadyBiSResult.class,
                res -> assertThat(res.item()).isEqualTo(topTrinket2));

        verify(powerCalculator, times(itemPool.size()))
                .calculate(argThat(itemPool::contains), eq(classSpec), eq(wowCharacter.race()));
    }

    @Test
    void shouldSignalAlreadyBiSAndUpgradeFoundResults_UniqueBiSAndWeakerItemTheOnlyOnesInPool() {
        // given
        Item topUniqueTrinket = ItemBuilder.newEquippableItem()
                .withBaseId(1L)
                .withInventoryType("TRINKET")
                .withUniqueEquipped(true)
                .build();
        Item otherTrinket = ItemBuilder.newEquippableItem().withBaseId(2L).withInventoryType("TRINKET").build();

        when(powerCalculator.calculate(topUniqueTrinket, classSpec, wowCharacter.race())).thenReturn(69.0);
        when(powerCalculator.calculate(otherTrinket, classSpec, wowCharacter.race())).thenReturn(21.37);

        List<Item> itemPool = List.of(topUniqueTrinket, otherTrinket);

        // when
        Map<Equipment.Slot, UpgradeResult> result = finder.findUpgrades(getCurrentTrinketPair(Optional.of(new EquippedItem(topUniqueTrinket, List.of())),
                Optional.empty()), wowCharacter, talents, itemPool);

        // then
        assertThat(result).containsOnlyKeys(Equipment.Slot.TRINKET_1, Equipment.Slot.TRINKET_2);

        assertThat(result.get(Equipment.Slot.TRINKET_1)).isInstanceOfSatisfying(UpgradeResult.AlreadyBiSResult.class,
                res -> assertThat(res.item()).isEqualTo(topUniqueTrinket));

        assertThat(result.get(Equipment.Slot.TRINKET_2)).isInstanceOfSatisfying(UpgradeResult.UpgradeFoundResult.class,
                res -> assertThat(res.item()).isEqualTo(otherTrinket));

        verify(powerCalculator, times(itemPool.size()))
                .calculate(argThat(itemPool::contains), eq(classSpec), eq(wowCharacter.race()));
    }

    @Test
    void shouldSignalUpgradeFoundEvents_BothUpgradesDifferentAndUniqueEquipped() {
        // given
        Item uniqueRing1 = ItemBuilder.newEquippableItem().withBaseId(1L)
                .withInventoryType("FINGER")
                .withUniqueEquipped(true)
                .build();
        Item uniqueRing2 = ItemBuilder.newEquippableItem().withBaseId(2L)
                .withInventoryType("FINGER")
                .withUniqueEquipped(true)
                .build();

        when(powerCalculator.calculate(uniqueRing1, classSpec, wowCharacter.race())).thenReturn(100.0);
        when(powerCalculator.calculate(uniqueRing2, classSpec, wowCharacter.race())).thenReturn(80.0);

        List<Item> itemPool = List.of(uniqueRing1, uniqueRing2);

        // when
        Map<Equipment.Slot, UpgradeResult> result = finder.findUpgrades(getCurrentRingPair(Optional.empty(),
                Optional.empty()), wowCharacter, talents, itemPool);

        // then
        assertThat(result).containsOnlyKeys(Equipment.Slot.FINGER_1, Equipment.Slot.FINGER_2);

        assertThat(result.get(Equipment.Slot.FINGER_1)).isInstanceOfSatisfying(UpgradeResult.UpgradeFoundResult.class,
                res -> assertThat(res.item()).isEqualTo(uniqueRing1));

        assertThat(result.get(Equipment.Slot.FINGER_2)).isInstanceOfSatisfying(UpgradeResult.UpgradeFoundResult.class,
                res -> assertThat(res.item()).isEqualTo(uniqueRing2));

        verify(powerCalculator, times(itemPool.size())).calculate(calculationCaptor.capture(), eq(classSpec), eq(wowCharacter.race()));
        verify(powerCalculator, times(itemPool.size()))
                .calculate(argThat(itemPool::contains), eq(classSpec), eq(wowCharacter.race()));
    }

    @Test
    void shouldAlwaysAssignItemsToSameSlots_SameTotalScoreOfCandidateItemsConfigurations() {
        // given
        Item ring1 = ItemBuilder.newEquippableItem().withBaseId(1L)
                .withInventoryType("FINGER")
                .withUniqueEquipped(true)
                .build();
        Item ring2 = ItemBuilder.newEquippableItem().withBaseId(2L).withInventoryType("FINGER").build();

        when(powerCalculator.calculate(ring1, classSpec, wowCharacter.race())).thenReturn(100.0);
        when(powerCalculator.calculate(ring2, classSpec, wowCharacter.race())).thenReturn(80.0);

        List<Item> itemPool = List.of(ring1, ring2);

        // when
        Map<Equipment.Slot, UpgradeResult> result = finder.findUpgrades(getCurrentRingPair(Optional.empty(),
                Optional.empty()), wowCharacter, talents, itemPool);

        // then
        assertThat(result).containsOnlyKeys(Equipment.Slot.FINGER_1, Equipment.Slot.FINGER_2);

        assertThat(result.get(Equipment.Slot.FINGER_1)).isInstanceOfSatisfying(UpgradeResult.UpgradeFoundResult.class,
                res -> assertThat(res.item()).isEqualTo(ring1));

        assertThat(result.get(Equipment.Slot.FINGER_2)).isInstanceOfSatisfying(UpgradeResult.UpgradeFoundResult.class,
                res -> assertThat(res.item()).isEqualTo(ring2));

        verify(powerCalculator, times(itemPool.size()))
                .calculate(argThat(itemPool::contains), eq(classSpec), eq(wowCharacter.race()));

    }

    @Test
    void shouldOptimizeCandidateItemsSlotAssignment_ByTotalScoreOfACandidateConfiguration() {
        // given
        Item bestRing = ItemBuilder.newEquippableItem().withBaseId(1L)
                .withInventoryType("FINGER")
                .withUniqueEquipped(true)
                .build();
        Item worstRing = ItemBuilder.newEquippableItem().withBaseId(2L).withInventoryType("FINGER").build();
        Item otherRing = ItemBuilder.newEquippableItem().withBaseId(3L).withInventoryType("FINGER").build();

        when(powerCalculator.calculate(bestRing, classSpec, wowCharacter.race())).thenReturn(80.0);
        when(powerCalculator.calculate(worstRing, classSpec, wowCharacter.race())).thenReturn(10.0);
        when(powerCalculator.calculate(otherRing, classSpec, wowCharacter.race())).thenReturn(50.0);

        EquippedItem slot1 = new EquippedItem(bestRing, List.of());
        EquippedItem slot2 = new EquippedItem(worstRing, List.of());

        List<Item> itemPool = List.of(bestRing, worstRing, otherRing);

        // when
        Map<Equipment.Slot, UpgradeResult> result = finder.findUpgrades(getCurrentRingPair(Optional.of(slot1), Optional.of(slot2)),
                wowCharacter, talents, itemPool);

        // then
        assertThat(result).containsOnlyKeys(Equipment.Slot.FINGER_1, Equipment.Slot.FINGER_2);

        assertThat(result.get(Equipment.Slot.FINGER_1)).isInstanceOfSatisfying(UpgradeResult.AlreadyBiSResult.class,
                res -> assertThat(res.item()).isEqualTo(bestRing));

        assertThat(result.get(Equipment.Slot.FINGER_2)).isInstanceOfSatisfying(UpgradeResult.UpgradeFoundResult.class,
                res -> assertThat(res.item()).isEqualTo(otherRing));

        verify(powerCalculator, times(itemPool.size()))
                .calculate(argThat(itemPool::contains), eq(classSpec), eq(wowCharacter.race()));
    }

    private EquippedItemPair getCurrentRingPair(Optional<EquippedItem> slot1, Optional<EquippedItem> slot2) {
        return new EquippedItemPair(
                new EquippedItemPair.Item(Equipment.Slot.FINGER_1, slot1),
                new EquippedItemPair.Item(Equipment.Slot.FINGER_2, slot2));
    }

    private EquippedItemPair getCurrentTrinketPair(Optional<EquippedItem> slot1, Optional<EquippedItem> slot2) {
        return new EquippedItemPair(
                new EquippedItemPair.Item(Equipment.Slot.TRINKET_1, slot1),
                new EquippedItemPair.Item(Equipment.Slot.TRINKET_2, slot2));
    }

    private WowTalents getTalents() {
        return new WowTalents(
                List.of(
                        new WowTalents.Specialization("COMBAT", 31),
                        new WowTalents.Specialization("ASSASSINATION", 20),
                        new WowTalents.Specialization("SUBTLETY", 0)
                ));
    }

}
