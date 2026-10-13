package com.bebis.BeBiS.engine.upgrade;

import com.bebis.BeBiS.engine.power.core.PowerCalculatorEngine;
import com.bebis.BeBiS.engine.upgrade.domain.UpgradeResult;
import com.bebis.BeBiS.equipment.domain.EquippedItem;
import com.bebis.BeBiS.item.EquippableItemBuilder;
import com.bebis.BeBiS.item.domain.Item;
import com.bebis.BeBiS.profile.domain.ClassSpec;
import com.bebis.BeBiS.profile.domain.WowCharacter;
import com.bebis.BeBiS.profile.domain.WowCharacterBuilder;
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

import static com.bebis.BeBiS.engine.upgrade.SingleSlotUpgradeOrchestrator.SlotUpgradeQuery;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SingleSlotUpgradeFinderTest {

    @Mock
    private PowerCalculatorEngine powerCalculator;

    private SingleSlotUpgradeFinder finder;

    @Captor
    private ArgumentCaptor<Item> calculationCaptor;

    // test data
    private final WowCharacter wowCharacter = WowCharacterBuilder.newWowCharacter().build();

    private final ClassSpec classSpec = ClassSpec.ROGUE_COMBAT;

    @BeforeEach
    void setup() {
        finder = new SingleSlotUpgradeFinder(powerCalculator, new UpgradeResultEvaluator());
    }

    @Test
    void shouldSignalUpgradeFoundResult_EmptyEquippedItem() {
        // given
        Item worseItem = EquippableItemBuilder.newRing().withBaseId(1L).build();
        Item betterItem = EquippableItemBuilder.newRing().withBaseId(2L).build();

        when(powerCalculator.calculate(worseItem, classSpec)).thenReturn(21.0);
        when(powerCalculator.calculate(betterItem, classSpec)).thenReturn(37.0);

        List<Item> itemPool = List.of(worseItem, betterItem);

        // when
        UpgradeResult result = finder.findUpgrade(new SlotUpgradeQuery(Optional.empty(), itemPool), classSpec);

        // then
        assertThat(result).asInstanceOf(type(UpgradeResult.UpgradeFoundResult.class))
                .extracting(UpgradeResult.UpgradeFoundResult::item).isEqualTo(betterItem);

        // since equippedItem is null the finder should have only run calculation as many times as number of items in the pool
        verify(powerCalculator, times(itemPool.size())).calculate(calculationCaptor.capture(), eq(classSpec));
        assertThat(calculationCaptor.getAllValues()).containsExactlyInAnyOrderElementsOf(itemPool);
    }

    @Test
    void shouldSignalNoItemsAvailableResult_EmptyItemPool() {
        // given
        List<Item> itemPool = List.of();

        // when
        UpgradeResult result = finder.findUpgrade(new SlotUpgradeQuery(Optional.empty(), itemPool), classSpec);

        // then
        assertThat(result).isInstanceOf(UpgradeResult.NoItemsAvailableResult.class);

        verify(powerCalculator, never()).calculate(any(Item.class), eq(classSpec));
    }

    @Test
    void shouldSignalUpgradeFoundResult_EquippedItemWorseThanBestItemInPool() {
        // given
        Item currentItem = EquippableItemBuilder.newRing().withBaseId(1L).build();
        EquippedItem equippedItem = new EquippedItem(currentItem, List.of());

        Item otherItem1 = EquippableItemBuilder.newRing().withBaseId(2L).build();
        Item bestItem = EquippableItemBuilder.newRing().withBaseId(3L).build();

        when(powerCalculator.calculate(currentItem, classSpec)).thenReturn(21.0);
        when(powerCalculator.calculate(otherItem1, classSpec)).thenReturn(37.0);
        when(powerCalculator.calculate(bestItem, classSpec)).thenReturn(69.0);

        List<Item> itemPool = List.of(currentItem, otherItem1, bestItem);

        // when
        UpgradeResult result = finder.findUpgrade(new SlotUpgradeQuery(Optional.of(equippedItem), itemPool), classSpec);

        // then
        assertThat(result).asInstanceOf(type(UpgradeResult.UpgradeFoundResult.class))
                .extracting(UpgradeResult.UpgradeFoundResult::item).isEqualTo(bestItem);

        verify(powerCalculator, times(itemPool.size())).calculate(calculationCaptor.capture(), eq(classSpec));
        assertThat(calculationCaptor.getAllValues()).containsExactlyInAnyOrderElementsOf(List.of(currentItem, otherItem1, bestItem));
    }

    @ParameterizedTest
    @MethodSource("scoreProvider")
    void shouldSignalAlreadyBiSResult_EquippedItemIsBiSOrEqualToAnotherItemInPool(double eqItemScore, double otherItemScore) {
        // given
        Item currentItem = EquippableItemBuilder.newRing().withBaseId(1L).build();
        EquippedItem equippedItem = new EquippedItem(currentItem, List.of());

        Item otherItem = EquippableItemBuilder.newRing().withBaseId(2L).build();

        when(powerCalculator.calculate(currentItem, classSpec)).thenReturn(eqItemScore);
        when(powerCalculator.calculate(otherItem, classSpec)).thenReturn(otherItemScore);

        List<Item> itemPool = List.of(currentItem, otherItem);

        // when
        UpgradeResult result = finder.findUpgrade(new SlotUpgradeQuery(Optional.of(equippedItem), itemPool), classSpec);

        // then
        assertThat(result).asInstanceOf(type(UpgradeResult.AlreadyBiSResult.class))
                .extracting(UpgradeResult.AlreadyBiSResult::item).isEqualTo(currentItem);

        verify(powerCalculator, times(itemPool.size())).calculate(calculationCaptor.capture(), eq(classSpec));
        assertThat(calculationCaptor.getAllValues()).containsExactlyInAnyOrderElementsOf(List.of(currentItem, otherItem));
    }

    private static Stream<Arguments> scoreProvider() {
        return Stream.of(
                Arguments.of(37.0, 21.0),
                Arguments.of(21.0, 21.0)
        );
    }
}
