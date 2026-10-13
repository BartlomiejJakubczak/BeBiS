package com.bebis.BeBiS.engine.upgrade;

import com.bebis.BeBiS.engine.upgrade.domain.ScoredItem;
import com.bebis.BeBiS.engine.upgrade.domain.UpgradeResult;
import com.bebis.BeBiS.item.EquippableItemBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class UpgradeResultEvaluatorTest {

    private UpgradeResultEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new UpgradeResultEvaluator();
    }

    @Test
    void shouldReturnNoItemsAvailableResultWhenUpgradeIsEmpty() {
        // given / when
        UpgradeResult result = evaluator.evaluateUpgradeResult(Optional.empty(), Optional.empty());
        // then
        assertThat(result).isInstanceOf(UpgradeResult.NoItemsAvailableResult.class);
    }

    @Test
    void shouldReturnUpgradeFoundResultWhenUpgradeHasHigherScore() {
        // given
        ScoredItem equippedItem = new ScoredItem(EquippableItemBuilder.newRing().withBaseId(1L).build(), 21L);
        ScoredItem upgrade = new ScoredItem(EquippableItemBuilder.newRing().withBaseId(2L).build(), 37L);
        // when
        UpgradeResult result = evaluator.evaluateUpgradeResult(Optional.of(equippedItem), Optional.of(upgrade));
        // then
        assertThat(result).isInstanceOf(UpgradeResult.UpgradeFoundResult.class);
        UpgradeResult.UpgradeFoundResult upgradeFoundResult = (UpgradeResult.UpgradeFoundResult) result;
        assertThat(upgradeFoundResult.item()).isEqualTo(upgrade.item());
    }

    @Test
    void shouldReturnAlreadyBiSResultWhenEquippedItemHasHigherScore() {
        // given
        ScoredItem equippedItem = new ScoredItem(EquippableItemBuilder.newRing().withBaseId(1L).build(), 50L);
        ScoredItem upgrade = new ScoredItem(EquippableItemBuilder.newRing().withBaseId(2L).build(), 37L);
        // when
        UpgradeResult result = evaluator.evaluateUpgradeResult(Optional.of(equippedItem), Optional.of(upgrade));
        // then
        assertThat(result).isInstanceOf(UpgradeResult.AlreadyBiSResult.class);
    }

    @Test
    void shouldReturnUpgradeFoundResultWhenEquippedItemIsEmpty() {
        // given
        ScoredItem upgrade = new ScoredItem(EquippableItemBuilder.newRing().withBaseId(2L).build(), 37L);
        // when
        UpgradeResult result = evaluator.evaluateUpgradeResult(Optional.empty(), Optional.of(upgrade));
        // then
        assertThat(result).isInstanceOf(UpgradeResult.UpgradeFoundResult.class);
        UpgradeResult.UpgradeFoundResult upgradeFoundResult = (UpgradeResult.UpgradeFoundResult) result;
        assertThat(upgradeFoundResult.item()).isEqualTo(upgrade.item());
    }

    @Test
    void shouldReturnAlreadyBiSResultWhenItemsHaveTheSameScore() {
        // given
        double score = 69.0;
        ScoredItem equippedItem = new ScoredItem(EquippableItemBuilder.newRing().withBaseId(1L).build(), score);
        ScoredItem upgrade = new ScoredItem(EquippableItemBuilder.newRing().withBaseId(2L).build(), score);
        // when
        UpgradeResult result = evaluator.evaluateUpgradeResult(Optional.of(equippedItem), Optional.of(upgrade));
        // then
        assertThat(result).isInstanceOf(UpgradeResult.AlreadyBiSResult.class);
    }
}
