package com.bebis.BeBiS.engine.upgrade;

import com.bebis.BeBiS.engine.power.core.PowerCalculatorEngine;
import com.bebis.BeBiS.engine.upgrade.domain.UpgradeResult;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.bebis.BeBiS.profile.domain.WowTalents.Specialization;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SingleSlotUpgradeFinderTest {

    @Mock
    private PowerCalculatorEngine powerCalculator;

    private SingleSlotUpgradeFinder finder;

    @Captor
    private ArgumentCaptor<Item> calculationCaptor;

    @BeforeEach
    void setup() {
        finder = new SingleSlotUpgradeFinder(powerCalculator, new UpgradeResultEvaluator());
    }

    @Test
    void shouldSignalUpgradeFoundResult_EmptyEquippedItem() {
        // given
        WowCharacter wowCharacter = WowCharacterBuilder.newWowCharacter().build();
        Optional<WowTalents> talents = Optional.of(getTalents());

        ClassSpec classSpec = ClassSpec.getClassSpec(wowCharacter.wowClass(), talents);

        Item worseItem = ItemBuilder.newEquippableItem().withBaseId(1L).build();
        Item betterItem = ItemBuilder.newEquippableItem().withBaseId(2L).build();

        when(powerCalculator.calculate(worseItem, classSpec, wowCharacter.race())).thenReturn(21.0);
        when(powerCalculator.calculate(betterItem, classSpec, wowCharacter.race())).thenReturn(37.0);

        List<Item> itemPool = List.of(worseItem, betterItem);

        // when
        UpgradeResult result = finder.findUpgrade(Optional.empty(), wowCharacter, talents, itemPool);

        // then
        assertThat(result).isInstanceOf(UpgradeResult.UpgradeFoundResult.class);
        UpgradeResult.UpgradeFoundResult upgradeFoundResult = (UpgradeResult.UpgradeFoundResult) result;

        assertThat(upgradeFoundResult.item()).isEqualTo(betterItem);

        // since equippedItem is null the finder should have only run calculation as many times as number of items in the pool
        verify(powerCalculator, times(itemPool.size())).calculate(calculationCaptor.capture(), eq(classSpec), eq(wowCharacter.race()));
        assertThat(calculationCaptor.getAllValues()).containsExactlyInAnyOrderElementsOf(itemPool);
    }

    @Test
    void shouldSignalNoItemsAvailableResult_EmptyItemPool() {
        // given
        WowCharacter wowCharacter = WowCharacterBuilder.newWowCharacter().build();
        Optional<WowTalents> talents = Optional.of(getTalents());

        ClassSpec classSpec = ClassSpec.getClassSpec(wowCharacter.wowClass(), talents);

        List<Item> itemPool = List.of();

        // when
        UpgradeResult result = finder.findUpgrade(Optional.empty(), wowCharacter, talents, itemPool);

        // then
        assertThat(result).isInstanceOf(UpgradeResult.NoItemsAvailableResult.class);

        verify(powerCalculator, never()).calculate(any(Item.class), eq(classSpec), eq(wowCharacter.race()));
    }

    @Test
    void shouldSignalUpgradeFoundResult_EquippedItemWorseThanBestItemInPool() {
        // given
        WowCharacter wowCharacter = WowCharacterBuilder.newWowCharacter().build();
        Optional<WowTalents> talents = Optional.of(getTalents());

        ClassSpec classSpec = ClassSpec.getClassSpec(wowCharacter.wowClass(), talents);

        Item currentItem = ItemBuilder.newEquippableItem().withBaseId(1L).build();
        EquippedItem equippedItem = new EquippedItem(currentItem, List.of());

        Item otherItem1 = ItemBuilder.newEquippableItem().withBaseId(2L).build();
        Item bestItem = ItemBuilder.newEquippableItem().withBaseId(3L).build();

        when(powerCalculator.calculate(currentItem, classSpec, wowCharacter.race())).thenReturn(21.0);
        when(powerCalculator.calculate(otherItem1, classSpec, wowCharacter.race())).thenReturn(37.0);
        when(powerCalculator.calculate(bestItem, classSpec, wowCharacter.race())).thenReturn(69.0);

        List<Item> itemPool = List.of(currentItem, otherItem1, bestItem);

        // when
        UpgradeResult result = finder.findUpgrade(Optional.of(equippedItem), wowCharacter, talents, itemPool);

        // then
        assertThat(result).isInstanceOf(UpgradeResult.UpgradeFoundResult.class);
        UpgradeResult.UpgradeFoundResult upgradeFoundResult = (UpgradeResult.UpgradeFoundResult) result;

        assertThat(upgradeFoundResult.item()).isEqualTo(bestItem);

        // additional 1 calculation for the equipped item comparison logic
        verify(powerCalculator, times(itemPool.size() + 1)).calculate(calculationCaptor.capture(), eq(classSpec), eq(wowCharacter.race()));
        // currentItem is a part of the itemPool and is calculated again during evaluation
        assertThat(calculationCaptor.getAllValues()).containsExactlyInAnyOrderElementsOf(List.of(currentItem, currentItem, otherItem1, bestItem));
    }

    @ParameterizedTest
    @MethodSource("scoreProvider")
    void shouldSignalAlreadyBiSResult_EquippedItemIsBiSOrEqualToAnotherItemInPool(double eqItemScore, double otherItemScore) {
        // given
        WowCharacter wowCharacter = WowCharacterBuilder.newWowCharacter().build();
        Optional<WowTalents> talents = Optional.of(getTalents());

        ClassSpec classSpec = ClassSpec.getClassSpec(wowCharacter.wowClass(), talents);

        Item currentItem = ItemBuilder.newEquippableItem().withBaseId(1L).build();
        EquippedItem equippedItem = new EquippedItem(currentItem, List.of());

        Item otherItem = ItemBuilder.newEquippableItem().withBaseId(2L).build();

        when(powerCalculator.calculate(currentItem, classSpec, wowCharacter.race())).thenReturn(eqItemScore);
        when(powerCalculator.calculate(otherItem, classSpec, wowCharacter.race())).thenReturn(otherItemScore);

        List<Item> itemPool = List.of(currentItem, otherItem);

        // when
        UpgradeResult result = finder.findUpgrade(Optional.of(equippedItem), wowCharacter, talents, itemPool);

        // then
        assertThat(result).isInstanceOf(UpgradeResult.AlreadyBiSResult.class);

        // additional 1 calculation for the equipped item comparison logic
        verify(powerCalculator, times(itemPool.size() + 1)).calculate(calculationCaptor.capture(), eq(classSpec), eq(wowCharacter.race()));
        // currentItem is a part of the itemPool and is calculated again during evaluation
        assertThat(calculationCaptor.getAllValues()).containsExactlyInAnyOrderElementsOf(List.of(currentItem, currentItem, otherItem));
    }

    private static Stream<Arguments> scoreProvider() {
        return Stream.of(
                Arguments.of(37.0, 21.0),
                Arguments.of(21.0, 21.0)
        );
    }

    private WowTalents getTalents() {
        return new WowTalents(
                List.of(
                        new Specialization("COMBAT", 31),
                        new Specialization("ASSASSINATION", 20),
                        new Specialization("SUBTLETY", 0)
                ));
    }
}
