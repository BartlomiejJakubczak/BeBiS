package com.bebis.BeBiS.engine.upgrade.domain;

import com.bebis.BeBiS.item.domain.Item;

import java.util.Optional;

import static com.bebis.BeBiS.item.domain.Item.InventoryType.TWO_HAND;

/**
 * A candidate (or the current) filling of two related slots: rings, trinkets, or main hand and off hand. Either slot
 * can be empty, e.g. the off hand next to a two-hander.
 */
public record SlotPair(Optional<ScoredItem> slot1, Optional<ScoredItem> slot2) {

    public Optional<ScoredItem> twoHandMainHand() {
        return slot1().filter((main) -> main.item().getMetadata().inventoryType().equals(TWO_HAND));
    }

    public int slotsEquippedCount() {
        return slot1().map((it) -> 1).orElse(0) + slot2().map((it) -> 1).orElse(0);
    }

    public double totalScore() {
        return slot1().map(ScoredItem::score).orElse(0.0) + slot2().map(ScoredItem::score).orElse(0.0);
    }

    /**
     * Whether both slots hold the same item and that item can't be worn twice (see {@link Item#isEquippableOnce()}).
     * Such a pair is impossible for a player and must never be suggested.
     */
    public boolean areSameOnceEquippables() {
        return sameItem(slot1(), slot2()).map(Item::isEquippableOnce).orElse(false);
    }

    /**
     * @return how many slots (0–2) hold the same item as the equipped pair in the same slot. Two empty slots don't
     * count. Used as a tie-breaker, so equal-scoring suggestions don't make the player swap items for nothing.
     */
    public int countEquippedMatches(SlotPair equipped) {
        return sameItem(this.slot1(), equipped.slot1()).map(it -> 1).orElse(0)
                + sameItem(this.slot2(), equipped.slot2()).map(it -> 1).orElse(0);
    }

    private Optional<Item> sameItem(Optional<ScoredItem> a, Optional<ScoredItem> b) {
        return a.isPresent() && b.isPresent() && a.get().item().equals(b.get().item())
                ? Optional.of(a.get().item())
                : Optional.empty();
    }
}
