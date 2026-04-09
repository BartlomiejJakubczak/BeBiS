package com.bebis.BeBiS.item;

import com.bebis.BeBiS.integration.blizzard.dto.ItemResponse;
import com.bebis.BeBiS.item.dto.ItemSyncData;
import com.bebis.BeBiS.item.jpa.ArmorEntity;
import com.bebis.BeBiS.item.jpa.ItemEntity;
import com.bebis.BeBiS.item.jpa.ItemEntityFactory;
import com.bebis.BeBiS.item.jpa.WeaponEntity;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ItemMapperTest {
    private final ItemMapper itemMapper = new ItemMapper();
    private final ItemEntityFactory itemEntityFactory = new ItemEntityFactory();
    private static final long DEFAULT_ENCH_ID = 0L;

    // --- SYNC DATA TESTS (DTO -> SyncData) ---

    @Test
    void shouldMapWeaponSyncDataCorrectly() {
        ItemResponse response = ItemTestData.thunderfuryResponse();
        ItemSyncData result = itemMapper.mapToSyncData(response, DEFAULT_ENCH_ID);

        assertTrue(result.isWeapon());
        assertEquals(1.9, result.speed());
        assertEquals(Weapon.WeaponType.SWORD, result.weaponType());
    }

    @Test
    void shouldNormalizeHighWeaponSpeed() {
        // Assume thunderfuryHighSpeedResponse returns speed as 1900.0
        ItemResponse response = ItemTestData.thunderfuryResponse();
        ItemSyncData result = itemMapper.mapToSyncData(response, DEFAULT_ENCH_ID);

        assertEquals(1.9, result.speed(), "Should divide by 1000 if speed is > 100");
    }

    @Test
    void shouldCaptureArmorOnNonArmorItems() {
        // A ring that surprisingly has armor
        Integer expectedArmorValue = 150;
        ItemResponse response = ItemTestData.equippableItemResponse(123, "Greatseal", "RING", expectedArmorValue);
        ItemSyncData result = itemMapper.mapToSyncData(response, DEFAULT_ENCH_ID);

        assertEquals(expectedArmorValue, result.armorValue(), "Should capture armor even on a ring");
    }

    // --- DOMAIN TESTS (Entity -> Domain) ---

    @Test
    void shouldMapWeaponEntityToDomainWeapon() {
        // given
        WeaponEntity entity = new WeaponEntity();
        entity.setId(new ItemEntity.CompositeKey(19019, 0L));
        entity.setName("Thunderfury");
        entity.setSpeed(1.9);
        entity.setMinDamage(44);
        entity.setMaxDamage(115);
        entity.setWeaponType(Weapon.WeaponType.SWORD);
        entity.setStats(Map.of(StatType.STRENGTH, 16));

        // when
        Item result = itemMapper.mapToDomain(entity);

        // then
        assertInstanceOf(Weapon.class, result);
        Weapon w = (Weapon) result;
        assertEquals(1.9, w.getSpeed());
        assertEquals(16, w.getMetadata().stats().get(StatType.STRENGTH));
    }

    @Test
    void shouldMapArmorEntityToDomainArmor() {
        // given
        ArmorEntity entity = new ArmorEntity();
        entity.setId(new ItemEntity.CompositeKey(123, 0L));
        entity.setArmorValue(500);
        entity.setArmorType(Armor.ArmorType.PLATE);
        entity.setInventoryType(Item.InventoryType.CHEST);

        // when
        Item result = itemMapper.mapToDomain(entity);

        // then
        assertInstanceOf(Armor.class, result);
        Armor a = (Armor) result;
        assertEquals(500, a.getArmorValue());
        assertEquals(Item.InventoryType.CHEST, a.getMetadata().inventoryType());
    }

    @Test
    void shouldHandleUnknownStatsInDomainMapping() {
        WeaponEntity entity = new WeaponEntity();
        entity.setId(new ItemEntity.CompositeKey(1, 0L));
        // Using a stats map that might be empty or null
        entity.setStats(null);

        Item result = itemMapper.mapToDomain(entity);
        assertNotNull(result.getMetadata().stats(), "Stats map should be empty, not null");
        assertTrue(result.getMetadata().stats().isEmpty());
    }
}