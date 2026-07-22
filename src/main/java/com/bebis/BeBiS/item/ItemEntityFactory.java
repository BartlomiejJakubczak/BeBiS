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
        setCommonMetadata(entity, syncData);
        return entity;
    }

    private ItemEntity initializeSubtype(ItemSyncData syncData) {
        if (syncData.isWeapon()) {
            WeaponEntity weapon = new WeaponEntity();
            weapon.setSpeed(syncData.weapon().speed());
            weapon.setMinDamage(syncData.weapon().minDamage());
            weapon.setMaxDamage(syncData.weapon().maxDamage());
            weapon.setDps(syncData.weapon().dps());
            weapon.setWeaponType(syncData.weapon().weaponType());
            return weapon;
        }

        if (syncData.isArmor()) {
            ArmorEntity armor = new ArmorEntity();
            armor.setArmorType(syncData.armor().armorType());
            return armor;
        }

        return new EquippableItemEntity();
    }

    private void setCommonMetadata(ItemEntity entity, ItemSyncData syncData) {
        entity.setPk(new ItemEntity.CompositeKey(syncData.commonData().baseId(), syncData.commonData().suffixId()));
        entity.setName(syncData.commonData().name());
        entity.setQuality(syncData.commonData().quality());
        entity.setInventoryType(syncData.commonData().inventoryType());
        entity.setItemLevel(syncData.commonData().itemLevel());
        entity.setRequiredLevel(syncData.commonData().requiredLevel());
        entity.setUniqueEquipped(syncData.commonData().uniqueEquipped());
        entity.setStats(syncData.commonData().stats() != null ? syncData.commonData().stats() : new HashMap<>());
        entity.setSpecialEffects(syncData.commonData().specialEffects() != null ? syncData.commonData().specialEffects() : new ArrayList<>());
    }

}
