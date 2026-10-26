package com.bebis.BeBiS.engine.upgrade.domain;

import com.bebis.BeBiS.item.domain.Item;

import java.util.List;

/**
 * The candidate items for one slot and one character, before scoring: already filtered to what the character can equip
 * and obtain (see {@code ItemPoolProvider}). Unordered and immutable.
 */
public record ItemPool(List<Item> items) {
    public ItemPool {
        items = List.copyOf(items);
    }
}
