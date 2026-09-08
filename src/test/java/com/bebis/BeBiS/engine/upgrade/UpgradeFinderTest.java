package com.bebis.BeBiS.engine.upgrade;

import com.bebis.BeBiS.engine.power.core.PowerCalculatorEngine;
import com.bebis.BeBiS.equipment.domain.EquippedItem;
import com.bebis.BeBiS.item.ItemBuilder;
import com.bebis.BeBiS.item.domain.Item;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

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

    @Test
    void shouldExtractEquippedItemFromItemPool() {
        // given
        EquippedItem equippedItem = new EquippedItem(ItemBuilder.newEquippableItem().withBaseId(1L).build(), List.of());
        Item otherItem1 = ItemBuilder.newEquippableItem().withBaseId(2L).build();
        Item otherItem2 = ItemBuilder.newEquippableItem().withBaseId(3L).build();

        // when
        Optional<ScoredItem> result = finder.extractEquippedItemFromPool(Optional.of(equippedItem),
                List.of(new ScoredItem(equippedItem.item(), 21.0), new ScoredItem(otherItem1, 37.0), new ScoredItem(otherItem2, 69.0)));

        // then
        assertThat(result.isPresent()).isTrue();
        assertThat(result.get().item()).isEqualTo(equippedItem.item());
    }

    @Test
    void shouldScoreItemUsingCalculator() {
        // given
        Item item = ItemBuilder.newEquippableItem().withBaseId(1L).build();
        double expectedScore = 42.0;

        when(calculator.calculate(eq(item), eq(ROGUE_COMBAT), eq(NIGHT_ELF)))
                .thenReturn(expectedScore);

        // when
        ScoredItem result = finder.scoreItem(item, ROGUE_COMBAT, NIGHT_ELF);

        // then
        assertThat(result.item()).isEqualTo(item);
        assertThat(result.score()).isEqualTo(expectedScore);

        // verifies exact delegation to the engine
        verify(calculator).calculate(item, ROGUE_COMBAT, NIGHT_ELF);
    }

    @Test
    void shouldScoreAndSortItemsInDescendingOrder() {
        // given
        Item lowItem = ItemBuilder.newEquippableItem().withBaseId(1L).build();
        Item midItem = ItemBuilder.newEquippableItem().withBaseId(2L).build();
        Item highItem = ItemBuilder.newEquippableItem().withBaseId(3L).build();

        when(calculator.calculate(eq(lowItem), eq(ROGUE_COMBAT), eq(NIGHT_ELF))).thenReturn(10.0);
        when(calculator.calculate(eq(midItem), eq(ROGUE_COMBAT), eq(NIGHT_ELF))).thenReturn(50.0);
        when(calculator.calculate(eq(highItem), eq(ROGUE_COMBAT), eq(NIGHT_ELF))).thenReturn(100.0);

        // when
        List<ScoredItem> result = finder.scoreAndSortItems(
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

    @Test
    void shouldMaintainInputOrderWhenScoresAreTied() {
        // given
        Item firstTiedItem = ItemBuilder.newEquippableItem().withBaseId(1L).build();
        Item secondTiedItem = ItemBuilder.newEquippableItem().withBaseId(2L).build();

        double tiedScore = 50.0;
        when(calculator.calculate(eq(firstTiedItem), eq(ROGUE_COMBAT), eq(NIGHT_ELF))).thenReturn(tiedScore);
        when(calculator.calculate(eq(secondTiedItem), eq(ROGUE_COMBAT), eq(NIGHT_ELF))).thenReturn(tiedScore);

        // when
        List<ScoredItem> result = finder.scoreAndSortItems(
                List.of(firstTiedItem, secondTiedItem),
                ROGUE_COMBAT,
                NIGHT_ELF
        );

        // then
        assertThat(result)
                .extracting(ScoredItem::item)
                .containsExactly(firstTiedItem, secondTiedItem);
    }

    @Test
    void shouldPreserveZeroScores() {
        // given
        Item zeroItem = ItemBuilder.newEquippableItem().withBaseId(1L).build();
        Item otherItem = ItemBuilder.newEquippableItem().withBaseId(2L).build();
        Item otherItem1 = ItemBuilder.newEquippableItem().withBaseId(3L).build();

        when(calculator.calculate(eq(zeroItem), eq(ROGUE_COMBAT), eq(NIGHT_ELF))).thenReturn(0.0);
        when(calculator.calculate(eq(otherItem), eq(ROGUE_COMBAT), eq(NIGHT_ELF))).thenReturn(10.0);
        when(calculator.calculate(eq(otherItem1), eq(ROGUE_COMBAT), eq(NIGHT_ELF))).thenReturn(50.0);

        // when
        List<ScoredItem> result = finder.scoreAndSortItems(
                List.of(otherItem1, zeroItem, otherItem),
                ROGUE_COMBAT,
                NIGHT_ELF
        );

        // then
        assertThat(result)
                .extracting(ScoredItem::score)
                .containsExactly(50.0, 10.0, 0.0);

        assertThat(result)
                .extracting(ScoredItem::item)
                .containsExactly(otherItem1, otherItem, zeroItem);
    }

    @Test
    void shouldBreakTiesDeterministicallyByBaseId() {
        // given
        Item higherIdItem = ItemBuilder.newEquippableItem().withBaseId(99L).build();
        Item lowerIdItem = ItemBuilder.newEquippableItem().withBaseId(10L).build();

        double tiedScore = 50.0;
        when(calculator.calculate(eq(higherIdItem), eq(ROGUE_COMBAT), eq(NIGHT_ELF))).thenReturn(tiedScore);
        when(calculator.calculate(eq(lowerIdItem), eq(ROGUE_COMBAT), eq(NIGHT_ELF))).thenReturn(tiedScore);

        // when (input order puts higher ID first)
        List<ScoredItem> result = finder.scoreAndSortItems(
                List.of(higherIdItem, lowerIdItem),
                ROGUE_COMBAT,
                NIGHT_ELF
        );

        // then (lower baseId wins deterministic tie-breaker)
        assertThat(result)
                .extracting(ScoredItem::item)
                .containsExactly(lowerIdItem, higherIdItem);
    }

    @Test
    void shouldReturnEmptyListWhenItemPoolIsEmpty() {
        // when
        List<ScoredItem> result = finder.scoreAndSortItems(
                List.of(),
                ROGUE_COMBAT,
                NIGHT_ELF
        );

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
