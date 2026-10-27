package com.bebis.BeBiS.engine.upgrade.domain;

import com.bebis.BeBiS.item.domain.Item;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Scored candidates for one slot, always ordered best first. The constructor enforces the order (score descending, then
 * {@code baseId} ascending, so ties are deterministic), so {@link #bestItem()} can be trusted however the pool was
 * built.
 */
public record ScoredItemPool(List<ScoredItem> items) {

    public ScoredItemPool {
        items = items.stream()
                .sorted(Comparator.comparingDouble(ScoredItem::score).reversed()
                        .thenComparingLong(item -> item.item().getMetadata().key().baseId())) // guarantees determinism
                .toList();
    }

    public Optional<ScoredItem> bestItem() {
        return items.stream().findFirst();
    }

    /**
     * @return the pool's scored entry for the given item, empty if the item isn't a candidate
     */
    public Optional<ScoredItem> findItem(Item item) {
        return items.stream().filter(it -> it.item().equals(item)).findFirst();
    }

    /**
     * @return a new pool with only the items of these inventory types, still ordered
     */
    public ScoredItemPool restrictedTo(List<Item.InventoryType> targetTypes) {
        return new ScoredItemPool(items.stream().filter((it) -> targetTypes.contains(it.item().getMetadata().inventoryType())).toList());
    }
}
