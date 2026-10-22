package com.bebis.BeBiS.item.domain;

import com.bebis.BeBiS.profile.domain.WowCharacter;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
public abstract sealed class Item permits Weapon, Armor, EquippableItem {

    public record ItemKey(long baseId, long suffixId) {
    }

    /**
     * Everything known about an item.
     *
     * @param playableClasses classes allowed to <b>equip</b> the item (from Blizzard), empty if every class can
     * @param sources         how the item can be obtained, empty if unknown (most items until drops, vendors etc. are
     *                        exported)
     */
    public record ItemMetadata(
            ItemKey key,
            String name,
            InventoryType inventoryType,
            Quality quality,
            int itemLevel,
            int requiredLevel,
            boolean uniqueEquipped,
            Map<StatType, Integer> stats,
            List<String> specialEffects,
            Set<WowCharacter.WowClass> playableClasses,
            Set<Source> sources
    ) {
    }

    protected ItemMetadata metadata;

    public Item(ItemMetadata metadata) {
        this.metadata = metadata;
    }

    public enum Quality {
        UNKNOWN,
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
        TABARD("Tabard"),
        WAIST("Waist"),
        LEGS("Legs"),
        FEET("Feet"),
        WRIST("Wrist"),
        HANDS("Hands"),
        FINGER("Finger"),
        TRINKET("Trinket"),
        CLOAK("Back"),

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
        RELIC("Relic");

        private final String displayName;

        InventoryType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * A way of obtaining an item. Sources carry <b>acquisition</b> constraints (who can get the item), as opposed to
     * the item's own equip constraints.
     */
    public sealed interface Source permits QuestSource, OtherSource {

        boolean allows(WowCharacter.Race race);

        boolean allows(WowCharacter.WowClass wowClass);
    }

    /**
     * Whether a character of this race can obtain the item: true if its sources are unknown, or if <b>any</b> source
     * allows the race (e.g. an Orc can obtain a reward of mirrored Alliance and Horde quests through the Horde one).
     */
    public boolean isObtainableBy(WowCharacter.Race race) {
        return metadata.sources().isEmpty() || metadata.sources().stream().anyMatch(source -> source.allows(race));
    }

    /**
     * Whether a character of this class can obtain the item, by the same "any source" rule as
     * {@link #isObtainableBy(WowCharacter.Race)}. Whether the class may equip it is a separate question, see
     * {@link ItemMetadata#playableClasses()}.
     */
    public boolean isObtainableBy(WowCharacter.WowClass wowClass) {
        return metadata.sources().isEmpty() || metadata.sources().stream().anyMatch(source -> source.allows(wowClass));
    }

    /**
     * Whether a character of this class can equip the item, derived directly from item response.
     */
    public boolean isEquippableBy(WowCharacter.WowClass wowClass) {
        return metadata.playableClasses().isEmpty() || metadata.playableClasses().contains(wowClass);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Item item)) return false;
        return Objects.equals(metadata.key(), item.metadata.key());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(metadata.key());
    }
}
