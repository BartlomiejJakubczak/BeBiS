package com.bebis.BeBiS.item;

import com.bebis.BeBiS.equipment.EquipmentTestData;
import com.bebis.BeBiS.integration.blizzard.dto.EquipmentResponse;
import com.bebis.BeBiS.integration.blizzard.dto.ItemResponse;
import com.bebis.BeBiS.item.dto.ItemSyncData;
import com.bebis.BeBiS.item.jpa.ArmorEntity;
import com.bebis.BeBiS.item.jpa.ItemEntity;
import com.bebis.BeBiS.item.jpa.WeaponEntity;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class ItemMapperTest {
    private final ItemMapper itemMapper = new ItemMapper();

    // --- SYNC DATA TESTS (DTO -> SyncData) ---

    @Test
    void shouldMapWeaponSyncDataCorrectly() {
        // given
        ItemResponse base = ItemTestData.thunderfuryResponse();
        EquipmentResponse.ItemDTO dto = EquipmentTestData.fromItemResponseNoSuffix(base, "MAIN_HAND", List.of());

        // when
        ItemSyncData result = itemMapper.mapToSyncData(base, dto);

        // then
        assertTrue(result.isWeapon());
        assertEquals(1.9, result.speed());
        assertEquals(Weapon.WeaponType.SWORD, result.weaponType());
    }

    @Test
    void shouldNormalizeHighWeaponSpeed() {
        // Assume thunderfuryHighSpeedResponse returns speed as 1900.0
        ItemResponse base = ItemTestData.thunderfuryResponse();
        EquipmentResponse.ItemDTO highSpeedDto = new EquipmentResponse.ItemDTO(
                new EquipmentResponse.ItemDTO.ItemDTOReference(base.id()),
                new EquipmentResponse.ItemDTO.SlotDTO("MAIN_HAND"),
                "Thunderfury",
                new EquipmentResponse.ItemDTO.QualityDTO("LEGENDARY"),
                new EquipmentResponse.ItemDTO.LevelDTO(80),
                List.of(),
                null,
                new EquipmentResponse.ItemDTO.WeaponDTO(
                        new EquipmentResponse.ItemDTO.WeaponDTO.DamageDTO(44, 115),
                        new EquipmentResponse.ItemDTO.WeaponDTO.AttackSpeedDTO(1900.0), // The high speed
                        new EquipmentResponse.ItemDTO.WeaponDTO.DpsDTO(53.9)
                ),
                List.of()
        );
        //when
        ItemSyncData result = itemMapper.mapToSyncData(base, highSpeedDto);

        // then
        assertEquals(1.9, result.speed(), "Should divide by 1000 if speed is > 100");
    }

    @Test
    void shouldMapArmorSyncDataCorrectly() {
        // given: Class 4, Subclass 4 (Plate)
        ItemResponse base = ItemTestData.armorResponse(1L, "Breastplate", 2137);
        EquipmentResponse.ItemDTO dto = EquipmentTestData.fromItemResponseNoSuffix(base, "CHEST", List.of());

        // when
        ItemSyncData result = itemMapper.mapToSyncData(base, dto);

        // then
        assertFalse(result.isWeapon());
        assertEquals(Armor.ArmorType.PLATE, result.armorType());
        assertNull(result.weaponType());
    }

    @Test
    void shouldMapEquippableItemSyncDataCorrectly() {
        // given: Class 4, Subclass 0 (Misc/Rings/Necks)
        ItemResponse base = ItemTestData.equippableItemResponse(123, "Greatseal", "FINGER", null);
        EquipmentResponse.ItemDTO dto = EquipmentTestData.fromItemResponseNoSuffix(base, "FINGER_1", List.of());

        // when
        ItemSyncData result = itemMapper.mapToSyncData(base, dto);

        // then
        assertFalse(result.isWeapon());
        assertNull(result.armorType(), "Misc items should not have a strict armor type assigned");
        assertNull(result.weaponType());
    }

    @Test
    void shouldCaptureArmorOnNonArmorItems() {
        Integer expectedArmorValue = 150;
        ItemResponse response = ItemTestData.equippableItemResponse(123, "Greatseal", "FINGER", expectedArmorValue);
        EquipmentResponse.ItemDTO dto = EquipmentTestData.fromItemResponseSuffixed(
                response, "FINGER_1", "UNCOMMON", "of The Monkey",
                37L, response.itemLevel() + 10, List.of(EquipmentTestData.stat("AGILITY", 5)), List.of()
        );

        ItemSyncData result = itemMapper.mapToSyncData(response, dto);

        assertEquals(expectedArmorValue, result.armorValue(), "Should capture armor even on a ring");
    }

    @Test
    void shouldKeepArmorNullWhenMissingInResponse() {
        // given: A Weapon DTO (weapons usually don't have an armor block)
        ItemResponse base = ItemTestData.thunderfuryResponse();
        EquipmentResponse.ItemDTO dto = EquipmentTestData.fromItemResponseNoSuffix(base, "MAIN_HAND", List.of());

        // when
        ItemSyncData result = itemMapper.mapToSyncData(base, dto);

        // then
        assertThat(result.armorValue()).isNull(); // Not 0!
    }

    @Test
    void shouldMapValidStatsAndIgnoreUnknownOnes() {
        // given
        ItemResponse base = ItemTestData.equippableItemResponse(123, "Greatseal", "FINGER", 2137);
        EquipmentResponse.ItemDTO dtoWithMixedStats = EquipmentTestData.fromItemResponseSuffixed(
                base, "FINGER_1", "UNCOMMON", "of The Monkey", 37L, base.itemLevel() + 10,
                List.of(
                        EquipmentTestData.stat("AGILITY", 15),
                        EquipmentTestData.stat("WEIRD_BLIZZARD_STAT_99", 100) // Unknown stat
                ),
                List.of()
        );

        // when
        ItemSyncData result = itemMapper.mapToSyncData(base, dtoWithMixedStats);

        // then
        assertThat(result.stats()).containsEntry(StatType.AGILITY, 15);
        assertThat(result.stats()).hasSize(1);
    }

    @Test
    void shouldMapNullUniqueEquippedToFalse() {
        // given
        ItemResponse base = ItemTestData.createDtoWithNulls();
        EquipmentResponse.ItemDTO dto = EquipmentTestData.fromItemResponseNoSuffix(base, "FINGER_1", List.of());

        // when
        ItemSyncData result = itemMapper.mapToSyncData(base, dto);

        // then
        assertThat(result.uniqueEquipped()).isFalse();
    }

    @Test
    void shouldThrowExceptionWhenNameIsMissing() {
        // given
        ItemResponse base = ItemTestData.createDtoWithNulls();
        EquipmentResponse.ItemDTO dto = new EquipmentResponse.ItemDTO(
                new EquipmentResponse.ItemDTO.ItemDTOReference(base.id()),
                new EquipmentResponse.ItemDTO.SlotDTO("FINGER_1"),
                null, // the null name,
                new EquipmentResponse.ItemDTO.QualityDTO(base.quality().type()),
                new EquipmentResponse.ItemDTO.LevelDTO(base.itemLevel()),
                null,
                null,
                null,
                List.of()
        );

        // when / then
        assertThrows(IllegalStateException.class, () -> itemMapper.mapToSyncData(base, dto));
    }

    @Test
    void shouldFallbackToUnknownForBadEnums() {
        // given
        ItemResponse base = ItemTestData.createDtoWithGarbageEnums("WEIRD_QUALITY", "WEIRD_INV_TYPE");
        EquipmentResponse.ItemDTO dto = EquipmentTestData.fromItemResponseNoSuffix(base, "FINGER_1", List.of());

        // when
        ItemSyncData result = itemMapper.mapToSyncData(base, dto);

        // then
        assertEquals(Item.Quality.UNKNOWN, result.quality());
        assertEquals(Item.InventoryType.UNKNOWN, result.inventoryType());
    }

    @Test
    void shouldMapSpecialEffectsCorrectly() {
        // given
        ItemResponse base = ItemTestData.thunderfuryResponse();
        EquipmentResponse.ItemDTO dto = EquipmentTestData.fromItemResponseNoSuffix(base, "MAIN_HAND", List.of());

        // when
        ItemSyncData result = itemMapper.mapToSyncData(base, dto);

        // then
        assertThat(result.specialEffects()).containsExactly(ItemTestData.TF_EFFECT);
    }

    // --- DOMAIN TESTS (Entity -> Domain) ---

    @Test
    void shouldMapWeaponEntityToDomainWeapon() {
        // given
        WeaponEntity entity = new WeaponEntity();
        entity.setPk(new ItemEntity.CompositeKey(19019L, 0L));
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
        entity.setPk(new ItemEntity.CompositeKey(123L, 0L));
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
        // given
        WeaponEntity entity = new WeaponEntity();
        entity.setPk(new ItemEntity.CompositeKey(1L, 0L));
        // Using a stats map that might be empty or null
        entity.setStats(null);

        // when
        Item result = itemMapper.mapToDomain(entity);

        assertNotNull(result.getMetadata().stats(), "Stats map should be empty, not null");
        assertTrue(result.getMetadata().stats().isEmpty());
    }

    @Test
    void shouldDefaultNullLevelsToZeroInDomain() {
        // given: An entity with null wrappers
        ArmorEntity entity = new ArmorEntity();
        entity.setPk(new ItemEntity.CompositeKey(1L, 0L));
        entity.setName("Broken Boots");
        entity.setItemLevel(null);     // Wrapper is null
        entity.setRequiredLevel(null); // Wrapper is null
        entity.setInventoryType(Item.InventoryType.FEET);
        entity.setQuality(Item.Quality.COMMON);

        // when
        Item result = itemMapper.mapToDomain(entity);

        // then
        assertEquals(0, result.getMetadata().itemLevel(), "Null Entity level should be 0 in Domain");
        assertEquals(0, result.getMetadata().requiredLevel());
    }

    // --- mapSuffixId ---

    @Test
    void shouldReturnSuffixIdWhenNameEndsWithOfSuffix() {
        // given
        long suffixId = 123L;
        ItemResponse base = ItemTestData.armorResponse(1L, "Bracers", 10);
        EquipmentResponse.ItemDTO dto = EquipmentTestData.fromItemResponseSuffixed(
                base, "WRISTS", "UNCOMMON", "of the Whale", suffixId, 20, List.of(), List.of()
        );

        // when
        long result = itemMapper.mapSuffixId(dto);

        // then
        assertEquals(suffixId, result, "Should extract the ID when name ends with suffix and starts with 'of '");
    }

    @Test
    void shouldReturnZeroWhenEnchantmentDoesNotStartWithOf() {
        // given
        ItemResponse base = ItemTestData.armorResponse(1L, "Stamina Bracers", 10);
        EquipmentResponse.ItemDTO dto = EquipmentTestData.fromItemResponseNoSuffix(
                base, "WRISTS", List.of(EquipmentTestData.enchant(999L, "+7 Stamina"))
        );

        // when
        long result = itemMapper.mapSuffixId(dto);

        // then
        assertEquals(0L, result, "Should ignore enchantments that don't start with 'of '");
    }

    @Test
    void shouldReturnZeroWhenNameDoesNotEndWithEnchantment() {
        // given: An enchantment "of the Tiger" exists, but the item name is just "Bracers"
        ItemResponse base = ItemTestData.armorResponse(1L, "Bracers", 10);
        EquipmentResponse.ItemDTO dto = EquipmentTestData.fromItemResponseNoSuffix(
                base, "WRISTS", List.of(EquipmentTestData.enchant(123L, "of the Tiger"))
        );

        // when
        long result = itemMapper.mapSuffixId(dto);

        // then
        assertEquals(0L, result, "Should ignore 'of ' enchantments if they aren't part of the item name");
    }

    @Test
    void shouldHandleNullEnchantmentsOrName() {
        // given
        EquipmentResponse.ItemDTO nullEnchs = new EquipmentResponse.ItemDTO(
                null, null, "Bracers", null, null, null, null, null, null
        );
        EquipmentResponse.ItemDTO nullName = new EquipmentResponse.ItemDTO(
                null, null, null, null, null, null, null, null, List.of()
        );

        // when / then
        assertEquals(0L, itemMapper.mapSuffixId(nullEnchs));
        assertEquals(0L, itemMapper.mapSuffixId(nullName));
    }

    @Test
    void shouldMatchSpecificSuffixAmongMultipleEnchantments() {
        // given
        long suffixId = 123L;
        ItemResponse base = ItemTestData.armorResponse(1L, "Bracers", 10);
        EquipmentResponse.ItemDTO dto = EquipmentTestData.fromItemResponseSuffixed(
                base, "WRISTS", "UNCOMMON", "of the Bear", suffixId, 20,
                List.of(),
                List.of(EquipmentTestData.enchant(2137L, "Crusader"))
        );

        // when
        long result = itemMapper.mapSuffixId(dto);

        // then
        assertEquals(suffixId, result, "Should correctly pick 'of the Bear' and ignore 'Crusader'");
    }
}