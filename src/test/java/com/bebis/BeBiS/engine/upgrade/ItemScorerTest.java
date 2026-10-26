package com.bebis.BeBiS.engine.upgrade;

import com.bebis.BeBiS.engine.power.core.PowerCalculatorEngine;
import com.bebis.BeBiS.engine.upgrade.domain.ItemPool;
import com.bebis.BeBiS.engine.upgrade.domain.ScoredItem;
import com.bebis.BeBiS.engine.upgrade.domain.ScoredItemPool;
import com.bebis.BeBiS.item.WeaponBuilder;
import com.bebis.BeBiS.item.domain.Item;
import com.bebis.BeBiS.profile.domain.ClassSpec;
import com.bebis.BeBiS.profile.domain.WowCharacter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.bebis.BeBiS.equipment.domain.Equipment.Slot.OFF_HAND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemScorerTest {

    @Mock
    private PowerCalculatorEngine calculator;

    private ItemScorer scorer;

    private final ClassSpec classSpec = ClassSpec.ROGUE_COMBAT;
    private final WowCharacter.Race race = WowCharacter.Race.HUMAN;

    @BeforeEach
    void setUp() {
        scorer = new ItemScorer(calculator);
    }

    @Test
    void shouldScoreEveryItemForTheSlotAndCharacter_IntoAnOrderedPool() {
        // given
        Item lowSword = WeaponBuilder.newOneHandSword().withBaseId(1L).build();
        Item highSword = WeaponBuilder.newOneHandSword().withBaseId(2L).build();

        when(calculator.calculate(OFF_HAND, lowSword, classSpec, race)).thenReturn(10.0);
        when(calculator.calculate(OFF_HAND, highSword, classSpec, race)).thenReturn(100.0);

        // when
        ScoredItemPool result = scorer.scoreItemPool(OFF_HAND, new ItemPool(List.of(lowSword, highSword)), classSpec, race);

        // then
        assertThat(result.items()).containsExactly(new ScoredItem(highSword, 100.0), new ScoredItem(lowSword, 10.0));
    }

    @Test
    void shouldNotCallCalculator_WhenPoolIsEmpty() {
        // when
        ScoredItemPool result = scorer.scoreItemPool(OFF_HAND, new ItemPool(List.of()), classSpec, race);

        // then
        assertThat(result.items()).isEmpty();
        verifyNoInteractions(calculator);
    }
}
