package com.bebis.BeBiS.engine.upgrade.domain;

import com.bebis.BeBiS.item.EquippableItemBuilder;
import com.bebis.BeBiS.item.domain.Item;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ItemPoolTest {

    @Test
    void shouldNotChange_WhenSourceListChangesAfterCreation() {
        // given
        Item ring = EquippableItemBuilder.newRing().withBaseId(1L).build();
        List<Item> source = new ArrayList<>(List.of(ring));

        // when
        ItemPool pool = new ItemPool(source);
        source.add(EquippableItemBuilder.newRing().withBaseId(2L).build());

        // then
        assertThat(pool.items()).containsExactly(ring);
    }

    @Test
    void shouldRejectModification_OfItsItems() {
        // given
        ItemPool pool = new ItemPool(List.of());

        // when / then
        assertThatThrownBy(() -> pool.items().add(EquippableItemBuilder.newRing().build()))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
