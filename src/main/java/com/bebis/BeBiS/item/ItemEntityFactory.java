package com.bebis.BeBiS.item;

import com.bebis.BeBiS.item.dto.ItemSyncData;
import com.bebis.BeBiS.item.jpa.ArmorEntity;
import com.bebis.BeBiS.item.jpa.EquippableItemEntity;
import com.bebis.BeBiS.item.jpa.ItemEntity;
import com.bebis.BeBiS.item.jpa.WeaponEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;

@Component
class ItemEntityFactory {

    ItemEntity createItemEntity(ItemSyncData syncData) {
        ItemEntity entity = initializeSubtype(syncData);
        mapCommonMetadata(entity, syncData);
        return entity;
    }

    private ItemEntity initializeSubtype(ItemSyncData syncData) {
        if (syncData.isWeapon()) {
            WeaponEntity weapon = new WeaponEntity();
            weapon.setSpeed(syncData.speed());
            weapon.setMinDamage(syncData.minDamage());
            weapon.setMaxDamage(syncData.maxDamage());
            weapon.setDps(syncData.dps());
            weapon.setWeaponType(syncData.weaponType());
            return weapon;
        }

        if (syncData.isArmor()) {
            ArmorEntity armor = new ArmorEntity();
            armor.setArmorType(syncData.armorType());
            return armor;
        }

        return new EquippableItemEntity();
    }

    private void mapCommonMetadata(ItemEntity entity, ItemSyncData syncData) {
        entity.setPk(new ItemEntity.CompositeKey(syncData.baseId(), syncData.suffixId()));
        entity.setName(syncData.name());
        entity.setQuality(syncData.quality());
        entity.setInventoryType(syncData.inventoryType());
        entity.setItemLevel(syncData.itemLevel());
        entity.setRequiredLevel(syncData.requiredLevel());
        entity.setUniqueEquipped(syncData.uniqueEquipped());
        entity.setStats(syncData.stats() != null ? syncData.stats() : new HashMap<>());
        entity.setSpecialEffects(syncData.specialEffects() != null ? syncData.specialEffects() : new ArrayList<>());
    }

}
