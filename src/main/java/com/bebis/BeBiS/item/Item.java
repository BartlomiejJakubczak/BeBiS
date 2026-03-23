package com.bebis.BeBiS.item;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@EqualsAndHashCode
public abstract sealed class Item permits Weapon, Armor, EquippableItem {

    public record ItemMetadata(
            long id,
            String name,
            InventoryType inventoryType,
            Quality quality,
            int itemLevel,
            int requiredLevel,
            boolean uniqueEquipped,
            Map<StatType, Integer> stats,
            List<String> specialEffects
    ) {
    }

    protected ItemMetadata metadata;

    public Item(ItemMetadata metadata) {
        this.metadata = metadata;
    }

    public enum Quality {
        POOR,
        COMMON,
        UNCOMMON,
        RARE,
        EPIC,
        LEGENDARY
    }

    public enum InventoryType {
        // Equipment
        HEAD("Head"),
        NECK("Neck"),
        SHOULDER("Shoulder"),
        BODY("Shirt"),
        CHEST("Chest"),
        WAIST("Waist"),
        LEGS("Legs"),
        FEET("Feet"),
        WRIST("Wrist"),
        HANDS("Hands"),
        FINGER("Finger"),
        TRINKET("Trinket"),
        CLOAK("Back"),
        UNKNOWN("Unknown"),

        // Weapons & Off-hands
        WEAPON("One-Hand"),
        SHIELD("Shield"),
        RANGED("Ranged"),
        TWO_HAND("Two-Hand"),
        WEAPONMAINHAND("Main Hand"),
        WEAPONOFFHAND("Off Hand"),
        HOLDABLE("Held In Off-hand"), // Usually caster off-hands
        THROWN("Thrown"),
        RANGEDRIGHT("Wands/Guns"),

        // Non-Equippables
        TABARD("Tabard"),
        NON_EQUIP("Non-equippable"),
        BAG("Bag");

        private final String displayName;

        InventoryType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
