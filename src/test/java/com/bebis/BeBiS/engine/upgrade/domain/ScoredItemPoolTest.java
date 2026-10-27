package com.bebis.BeBiS.engine.upgrade.domain;

import com.bebis.BeBiS.item.ArmorBuilder;
import com.bebis.BeBiS.item.EquippableItemBuilder;
import com.bebis.BeBiS.item.WeaponBuilder;
import com.bebis.BeBiS.item.domain.Item;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static com.bebis.BeBiS.item.domain.Item.InventoryType.SHIELD;
import static com.bebis.BeBiS.item.domain.Item.InventoryType.WEAPON;
import static org.assertj.core.api.Assertions.assertThat;

class ScoredItemPoolTest {

    private final Item ring1 = EquippableItemBuilder.newRing().withBaseId(1L).build();
    private final Item ring2 = EquippableItemBuilder.newRing().withBaseId(2L).build();
    private final Item ring3 = EquippableItemBuilder.newRing().withBaseId(3L).build();

    @Test
    void shouldOrderItemsByScoreDescending_RegardlessOfInputOrder() {
        // given / when
        ScoredItemPool pool = new ScoredItemPool(List.of(
                new ScoredItem(ring1, 10.0), new ScoredItem(ring3, 100.0), new ScoredItem(ring2, 50.0)));

        // then
        assertThat(pool.items()).extracting(ScoredItem::item).containsExactly(ring3, ring2, ring1);
    }

    @Test
    void shouldBreakScoreTiesByLowerBaseId_WhenScoresAreEqual() {
        // given / when (input order puts the higher baseId first)
        ScoredItemPool pool = new ScoredItemPool(List.of(new ScoredItem(ring3, 50.0), new ScoredItem(ring1, 50.0)));

        // then
        assertThat(pool.items()).extracting(ScoredItem::item).containsExactly(ring1, ring3);
    }

    @Test
    void shouldKeepZeroScoredItemsLast() {
        // given / when
        ScoredItemPool pool = new ScoredItemPool(List.of(
                new ScoredItem(ring1, 0.0), new ScoredItem(ring2, 50.0), new ScoredItem(ring3, 10.0)));

        // then
        assertThat(pool.items()).extracting(ScoredItem::score).containsExactly(50.0, 10.0, 0.0);
    }

    @Test
    void shouldReturnHighestScoredItem_AsBestItem() {
        // given
        ScoredItemPool pool = new ScoredItemPool(List.of(new ScoredItem(ring1, 10.0), new ScoredItem(ring2, 50.0)));

        // when / then
        assertThat(pool.bestItem()).map(ScoredItem::item).contains(ring2);
    }

    @Test
    void shouldReturnNoBestItem_WhenPoolIsEmpty() {
        // given / when / then
        assertThat(new ScoredItemPool(List.of()).bestItem()).isEmpty();
    }

    @Test
    void shouldFindScoredVersionOfItem_WhenItIsInPool() {
        // given
        ScoredItemPool pool = new ScoredItemPool(List.of(new ScoredItem(ring1, 21.0), new ScoredItem(ring2, 37.0)));

        // when
        Optional<ScoredItem> result = pool.findItem(ring1);

        // then
        assertThat(result).contains(new ScoredItem(ring1, 21.0));
    }

    @Test
    void shouldFindNothing_WhenItemIsNotInPool() {
        // given
        ScoredItemPool pool = new ScoredItemPool(List.of(new ScoredItem(ring2, 37.0)));

        // when / then
        assertThat(pool.findItem(ring1)).isEmpty();
    }

    @Test
    void shouldKeepOnlyItemsOfRequestedInventoryTypes_StillOrdered() {
        // given
        Item sword = WeaponBuilder.newOneHandSword().withBaseId(1L).build();
        Item otherSword = WeaponBuilder.newOneHandSword().withBaseId(2L).build();
        Item shield = ArmorBuilder.newShield().withBaseId(3L).build();

        ScoredItemPool pool = new ScoredItemPool(List.of(
                new ScoredItem(sword, 10.0), new ScoredItem(shield, 90.0), new ScoredItem(otherSword, 50.0)));

        // when
        ScoredItemPool weapons = pool.restrictedTo(List.of(WEAPON));

        // then
        assertThat(weapons.items()).extracting(ScoredItem::item).containsExactly(otherSword, sword);
        assertThat(pool.restrictedTo(List.of(SHIELD)).items()).extracting(ScoredItem::item).containsExactly(shield);
    }
}
