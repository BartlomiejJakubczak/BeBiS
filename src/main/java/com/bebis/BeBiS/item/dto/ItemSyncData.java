package com.bebis.BeBiS.item.dto;

import com.bebis.BeBiS.item.domain.Armor;
import com.bebis.BeBiS.item.domain.Item;
import com.bebis.BeBiS.item.domain.StatType;
import com.bebis.BeBiS.item.domain.Weapon;

import java.util.List;
import java.util.Map;

public record ItemSyncData(
        ItemSyncCommonData commonData,
        WeaponSyncData weapon,
        ArmorSyncData armor
) {
    public record ItemSyncCommonData(
            Long baseId,
            Long suffixId,
            String name,
            Item.Quality quality,
            Item.InventoryType inventoryType,
            Integer itemLevel,
            Integer requiredLevel,
            Boolean uniqueEquipped,
            Map<StatType, Integer> stats,
            List<String> specialEffects
    ) {
    }

    public record WeaponSyncData(Weapon.WeaponType weaponType, Double speed, Integer minDamage, Integer maxDamage,
                                 Double dps) {
    }

    public record ArmorSyncData(Armor.ArmorType armorType) {
    }

    public boolean isWeapon() {
        return weapon != null;
    }

    public boolean isArmor() {
        return armor != null;
    }

    public boolean isEquippable() {
        return !isWeapon() && !isArmor();
    }
}
