package com.bebis.BeBiS.engine.upgrade.domain;

import com.bebis.BeBiS.item.domain.Item;

import java.util.Optional;

import static com.bebis.BeBiS.item.domain.Item.InventoryType.TWO_HAND;

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

    public boolean hasUniqueConflict() {
        if (slot1().isPresent() && slot2().isPresent()) {
            Item it1 = slot1().get().item();
            return it1.equals(slot2().get().item()) && it1.getMetadata().uniqueEquipped();
        }
        return false;
    }
}
