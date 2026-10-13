package com.bebis.BeBiS.engine.upgrade;

import com.bebis.BeBiS.engine.power.core.PowerCalculatorEngine;
import com.bebis.BeBiS.engine.upgrade.domain.ScoredItem;
import com.bebis.BeBiS.equipment.domain.Equipment.Slot;
import com.bebis.BeBiS.equipment.domain.EquippedItem;
import com.bebis.BeBiS.item.EquippableItemBuilder;
import com.bebis.BeBiS.item.domain.Item;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.bebis.BeBiS.equipment.domain.Equipment.Slot.MAIN_HAND;
import static com.bebis.BeBiS.profile.domain.ClassSpec.ROGUE_COMBAT;
import static com.bebis.BeBiS.profile.domain.WowCharacter.Race.NIGHT_ELF;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UpgradeFinderTest {

    private UpgradeFinder finder;

    @Mock
    private PowerCalculatorEngine calculator;

    @BeforeEach
    public void setUp() {
        finder = new DummyUpgradeFinder(calculator);
    }

    // --- extractEquippedItemFromPool ---

    @Test
    void shouldExtractEquippedItemFromItemPoolWhenPresent() {
        // given
        EquippedItem equippedItem = new EquippedItem(EquippableItemBuilder.newRing().withBaseId(1L).build(), List.of());
        Item otherItem1 = EquippableItemBuilder.newRing().withBaseId(2L).build();
        Item otherItem2 = EquippableItemBuilder.newRing().withBaseId(3L).build();

        // when
        Optional<ScoredItem> result = finder.extractEquippedItemFromPool(
                Optional.of(equippedItem),
                List.of(
                        new ScoredItem(equippedItem.item(), 21.0),
                        new ScoredItem(otherItem1, 37.0),
                        new ScoredItem(otherItem2, 69.0)
                )
        );

        // then
        assertThat(result).isPresent();
        assertThat(result.get().item()).isEqualTo(equippedItem.item());
    }

    @Test
    void shouldReturnEmptyWhenEquippedItemNotInPool() {
        // given
        EquippedItem equippedItem = new EquippedItem(EquippableItemBuilder.newRing().withBaseId(1L).build(), List.of());
        Item otherItem = EquippableItemBuilder.newRing().withBaseId(2L).build();

        // when
        Optional<ScoredItem> result = finder.extractEquippedItemFromPool(
                Optional.of(equippedItem),
                List.of(new ScoredItem(otherItem, 37.0))
        );

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenEquippedItemIsEmpty() {
        // given
        Item item = EquippableItemBuilder.newRing().withBaseId(1L).build();

        // when
        Optional<ScoredItem> result = finder.extractEquippedItemFromPool(
                Optional.empty(),
                List.of(new ScoredItem(item, 37.0))
        );

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldScoreItemStatOnly() {
        // given
        Item item = EquippableItemBuilder.newRing().withBaseId(1L).build();
        double expectedScore = 42.0;

        when(calculator.calculate(eq(item), eq(ROGUE_COMBAT))).thenReturn(expectedScore);

        // when
        ScoredItem result = finder.scoreItem(item, ROGUE_COMBAT);

        // then
        assertThat(result.item()).isEqualTo(item);
        assertThat(result.score()).isEqualTo(expectedScore);
        verify(calculator).calculate(item, ROGUE_COMBAT);
    }

    @Test
    void shouldScoreAndSortItemsInDescendingOrderStatOnly() {
        // given
        Item lowItem = EquippableItemBuilder.newRing().withBaseId(1L).build();
        Item midItem = EquippableItemBuilder.newRing().withBaseId(2L).build();
        Item highItem = EquippableItemBuilder.newRing().withBaseId(3L).build();

        when(calculator.calculate(eq(lowItem), eq(ROGUE_COMBAT))).thenReturn(10.0);
        when(calculator.calculate(eq(midItem), eq(ROGUE_COMBAT))).thenReturn(50.0);
        when(calculator.calculate(eq(highItem), eq(ROGUE_COMBAT))).thenReturn(100.0);

        // when
        List<ScoredItem> result = finder.scoreAndSortItems(List.of(lowItem, highItem, midItem), ROGUE_COMBAT);

        // then
        assertThat(result)
                .extracting(ScoredItem::item)
                .containsExactly(highItem, midItem, lowItem);

        assertThat(result)
                .extracting(ScoredItem::score)
                .containsExactly(100.0, 50.0, 10.0);
    }

    @Test
    void shouldScoreItemSlotAware() {
        // given
        Slot slot = MAIN_HAND;
        Item item = EquippableItemBuilder.newRing().withBaseId(1L).build();
        double expectedScore = 84.0;

        when(calculator.calculate(eq(slot), eq(item), eq(ROGUE_COMBAT), eq(NIGHT_ELF))).thenReturn(expectedScore);

        // when
        ScoredItem result = finder.scoreItem(slot, item, ROGUE_COMBAT, NIGHT_ELF);

        // then
        assertThat(result.item()).isEqualTo(item);
        assertThat(result.score()).isEqualTo(expectedScore);
        verify(calculator).calculate(slot, item, ROGUE_COMBAT, NIGHT_ELF);
    }

    @Test
    void shouldScoreAndSortItemsInDescendingOrderSlotAware() {
        // given
        Slot slot = MAIN_HAND;
        Item lowItem = EquippableItemBuilder.newRing().withBaseId(1L).build();
        Item midItem = EquippableItemBuilder.newRing().withBaseId(2L).build();
        Item highItem = EquippableItemBuilder.newRing().withBaseId(3L).build();

        when(calculator.calculate(eq(slot), eq(lowItem), eq(ROGUE_COMBAT), eq(NIGHT_ELF))).thenReturn(10.0);
        when(calculator.calculate(eq(slot), eq(midItem), eq(ROGUE_COMBAT), eq(NIGHT_ELF))).thenReturn(50.0);
        when(calculator.calculate(eq(slot), eq(highItem), eq(ROGUE_COMBAT), eq(NIGHT_ELF))).thenReturn(100.0);

        // when
        List<ScoredItem> result = finder.scoreAndSortItems(
                slot,
                List.of(lowItem, highItem, midItem),
                ROGUE_COMBAT,
                NIGHT_ELF
        );

        // then
        assertThat(result)
                .extracting(ScoredItem::item)
                .containsExactly(highItem, midItem, lowItem);

        assertThat(result)
                .extracting(ScoredItem::score)
                .containsExactly(100.0, 50.0, 10.0);
    }

    // --- Deterministic Tie-Breaking & Edge Cases ---

    @Test
    void shouldBreakTiesDeterministicallyByBaseId() {
        // given
        Item higherIdItem = EquippableItemBuilder.newRing().withBaseId(99L).build();
        Item lowerIdItem = EquippableItemBuilder.newRing().withBaseId(10L).build();

        double tiedScore = 50.0;
        when(calculator.calculate(eq(higherIdItem), eq(ROGUE_COMBAT))).thenReturn(tiedScore);
        when(calculator.calculate(eq(lowerIdItem), eq(ROGUE_COMBAT))).thenReturn(tiedScore);

        // when (input order puts higher baseId first)
        List<ScoredItem> result = finder.scoreAndSortItems(List.of(higherIdItem, lowerIdItem), ROGUE_COMBAT);

        // then (lower baseId wins deterministic tie-breaker regardless of input order)
        assertThat(result)
                .extracting(ScoredItem::item)
                .containsExactly(lowerIdItem, higherIdItem);
    }

    @Test
    void shouldPreserveZeroScores() {
        // given
        Item zeroItem = EquippableItemBuilder.newRing().withBaseId(1L).build();
        Item otherItem = EquippableItemBuilder.newRing().withBaseId(2L).build();
        Item otherItem1 = EquippableItemBuilder.newRing().withBaseId(3L).build();

        when(calculator.calculate(eq(zeroItem), eq(ROGUE_COMBAT))).thenReturn(0.0);
        when(calculator.calculate(eq(otherItem), eq(ROGUE_COMBAT))).thenReturn(10.0);
        when(calculator.calculate(eq(otherItem1), eq(ROGUE_COMBAT))).thenReturn(50.0);

        // when
        List<ScoredItem> result = finder.scoreAndSortItems(List.of(otherItem1, zeroItem, otherItem), ROGUE_COMBAT);

        // then
        assertThat(result)
                .extracting(ScoredItem::score)
                .containsExactly(50.0, 10.0, 0.0);

        assertThat(result)
                .extracting(ScoredItem::item)
                .containsExactly(otherItem1, otherItem, zeroItem);
    }

    @Test
    void shouldReturnEmptyListWhenItemPoolIsEmpty() {
        // when
        List<ScoredItem> result = finder.scoreAndSortItems(List.of(), ROGUE_COMBAT);

        // then
        assertThat(result).isEmpty();
        verifyNoInteractions(calculator);
    }

    private static class DummyUpgradeFinder extends UpgradeFinder {

        DummyUpgradeFinder(PowerCalculatorEngine calculator) {
            super(calculator);
        }
    }
}
