package com.bebis.BeBiS.engine.upgrade;

import com.bebis.BeBiS.engine.power.core.PowerCalculatorEngine;
import com.bebis.BeBiS.item.ItemBuilder;
import com.bebis.BeBiS.item.domain.Item;
import com.bebis.BeBiS.profile.domain.ClassSpec;
import com.bebis.BeBiS.profile.domain.WowCharacter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

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
    void shouldExtractBestScoringItemFromThePool() {
        // given
        Item someItem = ItemBuilder.newEquippableItem().withBaseId(1L).build();
        Item otherItem = ItemBuilder.newEquippableItem().withBaseId(2L).build();
        Item bestItem = ItemBuilder.newEquippableItem().withBaseId(3L).build();

        double bestScore = 69.0;

        when(calculator.calculate(eq(someItem), eq(ClassSpec.ROGUE_COMBAT), eq(WowCharacter.Race.NIGHT_ELF))).thenReturn(21.0);
        when(calculator.calculate(eq(otherItem), eq(ClassSpec.ROGUE_COMBAT), eq(WowCharacter.Race.NIGHT_ELF))).thenReturn(37.0);
        when(calculator.calculate(eq(bestItem), eq(ClassSpec.ROGUE_COMBAT), eq(WowCharacter.Race.NIGHT_ELF))).thenReturn(bestScore);

        // when
        Optional<ScoredItem> result = finder.extractBestItemFromPool(List.of(someItem, otherItem, bestItem), ClassSpec.ROGUE_COMBAT, WowCharacter.Race.NIGHT_ELF);

        // then
        assertThat(result).isPresent();
        ScoredItem scoredItem = result.get();

        assertThat(scoredItem.item()).isEqualTo(bestItem);
        assertThat(scoredItem.score()).isEqualTo(bestScore);
    }

    @Test
    void shouldScoreItemUsingCalculator() {
        // given
        Item item = ItemBuilder.newEquippableItem().withBaseId(1L).build();
        double expectedScore = 42.0;

        when(calculator.calculate(eq(item), eq(ClassSpec.ROGUE_COMBAT), eq(WowCharacter.Race.NIGHT_ELF)))
                .thenReturn(expectedScore);

        // when
        ScoredItem result = finder.scoreItem(item, ClassSpec.ROGUE_COMBAT, WowCharacter.Race.NIGHT_ELF);

        // then
        assertThat(result.item()).isEqualTo(item);
        assertThat(result.score()).isEqualTo(expectedScore);

        // verifies exact delegation to the engine
        verify(calculator).calculate(item, ClassSpec.ROGUE_COMBAT, WowCharacter.Race.NIGHT_ELF);
    }

    @Test
    void shouldScoreAndSortItemsInDescendingOrder() {
        // given
        Item lowItem = ItemBuilder.newEquippableItem().withBaseId(1L).build();
        Item midItem = ItemBuilder.newEquippableItem().withBaseId(2L).build();
        Item highItem = ItemBuilder.newEquippableItem().withBaseId(3L).build();

        when(calculator.calculate(eq(lowItem), eq(ClassSpec.ROGUE_COMBAT), eq(WowCharacter.Race.NIGHT_ELF))).thenReturn(10.0);
        when(calculator.calculate(eq(midItem), eq(ClassSpec.ROGUE_COMBAT), eq(WowCharacter.Race.NIGHT_ELF))).thenReturn(50.0);
        when(calculator.calculate(eq(highItem), eq(ClassSpec.ROGUE_COMBAT), eq(WowCharacter.Race.NIGHT_ELF))).thenReturn(100.0);

        // when
        List<ScoredItem> result = finder.scoreAndSortItems(
                List.of(lowItem, highItem, midItem),
                ClassSpec.ROGUE_COMBAT,
                WowCharacter.Race.NIGHT_ELF
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
        when(calculator.calculate(eq(firstTiedItem), eq(ClassSpec.ROGUE_COMBAT), eq(WowCharacter.Race.NIGHT_ELF))).thenReturn(tiedScore);
        when(calculator.calculate(eq(secondTiedItem), eq(ClassSpec.ROGUE_COMBAT), eq(WowCharacter.Race.NIGHT_ELF))).thenReturn(tiedScore);

        // when
        List<ScoredItem> result = finder.scoreAndSortItems(
                List.of(firstTiedItem, secondTiedItem),
                ClassSpec.ROGUE_COMBAT,
                WowCharacter.Race.NIGHT_ELF
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

        when(calculator.calculate(eq(zeroItem), eq(ClassSpec.ROGUE_COMBAT), eq(WowCharacter.Race.NIGHT_ELF))).thenReturn(0.0);
        when(calculator.calculate(eq(otherItem), eq(ClassSpec.ROGUE_COMBAT), eq(WowCharacter.Race.NIGHT_ELF))).thenReturn(10.0);
        when(calculator.calculate(eq(otherItem1), eq(ClassSpec.ROGUE_COMBAT), eq(WowCharacter.Race.NIGHT_ELF))).thenReturn(50.0);

        // when
        List<ScoredItem> result = finder.scoreAndSortItems(
                List.of(otherItem1, zeroItem, otherItem),
                ClassSpec.ROGUE_COMBAT,
                WowCharacter.Race.NIGHT_ELF
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
        when(calculator.calculate(eq(higherIdItem), eq(ClassSpec.ROGUE_COMBAT), eq(WowCharacter.Race.NIGHT_ELF))).thenReturn(tiedScore);
        when(calculator.calculate(eq(lowerIdItem), eq(ClassSpec.ROGUE_COMBAT), eq(WowCharacter.Race.NIGHT_ELF))).thenReturn(tiedScore);

        // when (input order puts higher ID first)
        List<ScoredItem> result = finder.scoreAndSortItems(
                List.of(higherIdItem, lowerIdItem),
                ClassSpec.ROGUE_COMBAT,
                WowCharacter.Race.NIGHT_ELF
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
                ClassSpec.ROGUE_COMBAT,
                WowCharacter.Race.NIGHT_ELF
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
