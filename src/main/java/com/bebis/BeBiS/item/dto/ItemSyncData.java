package com.bebis.BeBiS.item.dto;

import com.bebis.BeBiS.item.Armor;
import com.bebis.BeBiS.item.Item;
import com.bebis.BeBiS.item.StatType;
import com.bebis.BeBiS.item.Weapon;

import java.util.List;
import java.util.Map;

public record ItemSyncData(
        Long baseId,
        Long suffixId,
        String name,
        Item.Quality quality,
        Item.InventoryType inventoryType,
        Integer itemLevel,
        Integer requiredLevel,
        Boolean uniqueEquipped,
        Map<StatType, Integer> stats,
        List<String> specialEffects,
        Integer armorValue,
        Armor.ArmorType armorType,
        Double speed,
        Integer minDamage,
        Integer maxDamage,
        Double dps,
        Weapon.WeaponType weaponType
) {
    public boolean isWeapon() {
        return weaponType != null;
    }

    public boolean isArmor() {
        return armorType != null;
    }

    public boolean isEquippable() {
        return !isWeapon() && !isArmor();
    }
}
