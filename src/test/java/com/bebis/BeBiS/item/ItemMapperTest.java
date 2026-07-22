package com.bebis.BeBiS.item;

import com.bebis.BeBiS.equipment.EquipmentTestData;
import com.bebis.BeBiS.integration.blizzard.dto.EquipmentResponse;
import com.bebis.BeBiS.integration.blizzard.dto.ItemResponse;
import com.bebis.BeBiS.item.domain.Armor;
import com.bebis.BeBiS.item.domain.EquippableItem;
import com.bebis.BeBiS.item.domain.Item;
import com.bebis.BeBiS.item.domain.StatType;
import com.bebis.BeBiS.item.domain.Weapon;
import com.bebis.BeBiS.item.domain.exception.InvalidItemException;
import com.bebis.BeBiS.item.dto.ItemSyncData;
import com.bebis.BeBiS.item.jpa.ArmorEntity;
import com.bebis.BeBiS.item.jpa.EquippableItemEntity;
import com.bebis.BeBiS.item.jpa.ItemEntity;
import com.bebis.BeBiS.item.jpa.WeaponEntity;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

class ItemMapperTest {

    private final ItemMapper itemMapper = new ItemMapper();

    @Nested
    class SyncDataMapping {

        @Test
        void shouldMapWeaponSyncDataCorrectly() {
            // given
            ItemResponse base = ItemTestData.thunderfuryResponse();
            EquipmentResponse.ItemDTO dto = EquipmentTestData.fromItemResponseNoSuffix(base, "MAIN_HAND", List.of());

            // when
            ItemSyncData result = itemMapper.mapToSyncData(base, dto);

            // then
            assertTrue(result.isWeapon());
            assertEquals(1.9, result.weapon().speed());
            assertEquals(Weapon.WeaponType.SWORD, result.weapon().weaponType());
        }

        @Test
        void shouldNormalizeHighWeaponSpeed() {
            // given
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
                            new EquipmentResponse.ItemDTO.WeaponDTO.AttackSpeedDTO(1900.0), // High speed in ms
                            new EquipmentResponse.ItemDTO.WeaponDTO.DpsDTO(53.9)
                    ),
                    List.of()
            );

            // when
            ItemSyncData result = itemMapper.mapToSyncData(base, highSpeedDto);

            // then
            assertEquals(1.9, result.weapon().speed(), "Should divide by 1000 if speed is > 100");
        }

        @Test
        void shouldMapArmorSyncDataCorrectly() {
            // given
            ItemResponse base = ItemTestData.armorResponse(1L, "Breastplate", 2137);
            EquipmentResponse.ItemDTO dto = EquipmentTestData.fromItemResponseNoSuffix(base, "CHEST", List.of());

            // when
            ItemSyncData result = itemMapper.mapToSyncData(base, dto);

            // then
            assertFalse(result.isWeapon());
            assertEquals(Armor.ArmorType.PLATE, result.armor().armorType());
        }

        @Test
        void shouldMapEquippableItemSyncDataCorrectly() {
            // given
            ItemResponse base = ItemTestData.equippableItemResponse(123, "Greatseal", "FINGER", null);
            EquipmentResponse.ItemDTO dto = EquipmentTestData.fromItemResponseNoSuffix(base, "FINGER_1", List.of());

            // when
            ItemSyncData result = itemMapper.mapToSyncData(base, dto);

            // then
            assertTrue(result.isEquippable());
        }

        @Test
        void shouldCaptureArmorOnNonArmorItems() {
            // given
            Integer expectedArmorValue = 150;
            ItemResponse response = ItemTestData.equippableItemResponse(123, "Greatseal", "FINGER", expectedArmorValue);
            EquipmentResponse.ItemDTO dto = EquipmentTestData.fromItemResponseSuffixed(
                    response, "FINGER_1", "UNCOMMON", "of The Monkey",
                    37L, response.itemLevel() + 10, List.of(EquipmentTestData.stat("AGILITY", 5)), List.of()
            );

            // when
            ItemSyncData result = itemMapper.mapToSyncData(response, dto);

            // then
            assertEquals(expectedArmorValue, result.commonData().stats().get(StatType.ARMOR), "Should capture armor even on a ring");
        }

        @Test
        void shouldKeepArmorNullWhenMissingInResponse() {
            // given
            ItemResponse base = ItemTestData.thunderfuryResponse();
            EquipmentResponse.ItemDTO dto = EquipmentTestData.fromItemResponseNoSuffix(base, "MAIN_HAND", List.of());

            // when
            ItemSyncData result = itemMapper.mapToSyncData(base, dto);

            // then
            assertThat(result.commonData().stats().get(StatType.ARMOR)).isNull();
        }

        @Test
        void shouldFallbackToBaseItemArmorWhenEquippedArmorIsNull() {
            // given
            int expectedArmor = 150;
            ItemResponse base = ItemTestData.equippableItemResponse(100L, "Shielding Ring", "FINGER", expectedArmor);
            EquipmentResponse.ItemDTO dto = EquipmentTestData.fromItemResponseNoSuffix(base, "FINGER_1", List.of());

            // when
            ItemSyncData result = itemMapper.mapToSyncData(base, dto);

            // then
            assertEquals(expectedArmor, result.commonData().stats().get(StatType.ARMOR));
        }

        @Test
        void shouldMapValidStatsAndIgnoreUnknownOnes() {
            // given
            int armorValue = 2137;
            int agiValue = 15;
            ItemResponse base = ItemTestData.equippableItemResponse(123, "Greatseal", "FINGER", armorValue);
            EquipmentResponse.ItemDTO dtoWithMixedStats = EquipmentTestData.fromItemResponseSuffixed(
                    base, "FINGER_1", "UNCOMMON", "of The Monkey", 37L, base.itemLevel() + 10,
                    List.of(
                            EquipmentTestData.stat("AGILITY", agiValue),
                            EquipmentTestData.stat("WEIRD_BLIZZARD_STAT_99", 100)
                    ),
                    List.of()
            );

            // when
            ItemSyncData result = itemMapper.mapToSyncData(base, dtoWithMixedStats);

            // then
            assertThat(result.commonData().stats()).containsExactlyInAnyOrderEntriesOf(Map.of(
                    StatType.AGILITY, agiValue,
                    StatType.ARMOR, armorValue
            ));
            assertThat(result.commonData().stats()).hasSize(2);
        }

        @Test
        void shouldMapNullUniqueEquippedToFalse() {
            // given
            ItemResponse base = ItemTestData.createDtoWithNulls();
            EquipmentResponse.ItemDTO dto = EquipmentTestData.fromItemResponseNoSuffix(base, "FINGER_1", List.of());

            // when
            ItemSyncData result = itemMapper.mapToSyncData(base, dto);

            // then
            assertThat(result.commonData().uniqueEquipped()).isFalse();
        }

        @Test
        void shouldFallbackToUnknownForBadEnums() {
            // given
            ItemResponse base = ItemTestData.createDtoWithGarbageEnums("WEIRD_QUALITY");
            EquipmentResponse.ItemDTO dto = EquipmentTestData.fromItemResponseNoSuffix(base, "FINGER_1", List.of());

            // when
            ItemSyncData result = itemMapper.mapToSyncData(base, dto);

            // then
            assertEquals(Item.Quality.UNKNOWN, result.commonData().quality());
        }

        @Test
        void shouldMapSpecialEffectsCorrectly() {
            // given
            ItemResponse base = ItemTestData.thunderfuryResponse();
            EquipmentResponse.ItemDTO dto = EquipmentTestData.fromItemResponseNoSuffix(base, "MAIN_HAND", List.of());

            // when
            ItemSyncData result = itemMapper.mapToSyncData(base, dto);

            // then
            assertThat(result.commonData().specialEffects()).containsExactly(ItemTestData.TF_EFFECT);
        }
    }

    @Nested
    class SyncDataValidation {

        @Test
        void shouldThrowExceptionWhenTopLevelInputsAreNull() {
            // given
            ItemResponse base = ItemTestData.thunderfuryResponse();
            EquipmentResponse.ItemDTO dto = EquipmentTestData.fromItemResponseNoSuffix(base, "MAIN_HAND", List.of());

            // when / then
            assertThatThrownBy(() -> itemMapper.mapToSyncData(null, dto))
                    .isInstanceOf(InvalidItemException.class)
                    .hasMessage("item and equippedItem responses cannot be both null");

            assertThatThrownBy(() -> itemMapper.mapToSyncData(base, null))
                    .isInstanceOf(InvalidItemException.class)
                    .hasMessage("item and equippedItem responses cannot be both null");
        }

        @Test
        void shouldThrowExceptionWhenClassOrSubclassIsNull() {
            // given
            ItemResponse baseMissingClass = ItemTestData.responseWithNullClassAndSubclass();
            EquipmentResponse.ItemDTO dto = EquipmentTestData.fromItemResponseNoSuffix(baseMissingClass, "MAIN_HAND", List.of());

            // when / then
            assertThatThrownBy(() -> itemMapper.mapToSyncData(baseMissingClass, dto))
                    .isInstanceOf(InvalidItemException.class)
                    .hasMessage("classId and subclassId cannot be both null");
        }

        @Test
        void shouldThrowExceptionForUnsupportedClassId() {
            // given
            ItemResponse baseContainer = ItemTestData.containerResponse(1L, "Bag");
            EquipmentResponse.ItemDTO dto = EquipmentTestData.fromItemResponseNoSuffix(baseContainer, "BAG", List.of());

            // when / then
            assertThatThrownBy(() -> itemMapper.mapToSyncData(baseContainer, dto))
                    .isInstanceOf(InvalidItemException.class)
                    .hasMessage("Invalid classId: 1");
        }

        @Test
        void shouldThrowExceptionWhenInventoryTypeIsMissingOrInvalid() {
            // given
            ItemResponse baseNullInv = ItemTestData.responseWithInventoryType(null);
            EquipmentResponse.ItemDTO dto1 = EquipmentTestData.fromItemResponseNoSuffix(baseNullInv, "FINGER_1", List.of());

            ItemResponse baseBadInv = ItemTestData.responseWithInventoryType("GARBAGE_SLOT_123");
            EquipmentResponse.ItemDTO dto2 = EquipmentTestData.fromItemResponseNoSuffix(baseBadInv, "FINGER_1", List.of());

            // when / then
            assertThatThrownBy(() -> itemMapper.mapToSyncData(baseNullInv, dto1))
                    .isInstanceOf(InvalidItemException.class)
                    .hasMessage("Null inventory type");

            assertThatThrownBy(() -> itemMapper.mapToSyncData(baseBadInv, dto2))
                    .isInstanceOf(InvalidItemException.class)
                    .hasMessageContaining("Could not map inventory type");
        }

        @Test
        void shouldThrowInvalidItemExceptionWhenNameIsMissing() {
            // given
            ItemResponse baseNoName = ItemTestData.responseWithNullName();
            EquipmentResponse.ItemDTO dto = EquipmentTestData.fromItemResponseNoSuffix(baseNoName, "FINGER_1", List.of());

            // when / then
            assertThatThrownBy(() -> itemMapper.mapToSyncData(baseNoName, dto))
                    .isInstanceOf(InvalidItemException.class)
                    .hasMessage("Critical data missing: name");
        }
    }

    @Nested
    class DomainMapping {

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
            entity.setStats(Map.of(StatType.ARMOR, 500));
            entity.setArmorType(Armor.ArmorType.PLATE);
            entity.setInventoryType(Item.InventoryType.CHEST);

            // when
            Item result = itemMapper.mapToDomain(entity);

            // then
            assertInstanceOf(Armor.class, result);
            Armor a = (Armor) result;
            assertEquals(500, a.getMetadata().stats().get(StatType.ARMOR));
            assertEquals(Item.InventoryType.CHEST, a.getMetadata().inventoryType());
        }

        @Test
        void shouldMapEquippableItemEntityToDomainEquippableItem() {
            // given
            EquippableItemEntity entity = new EquippableItemEntity();
            entity.setPk(new ItemEntity.CompositeKey(999L, 0L));
            entity.setName("Band of Accuria");
            entity.setInventoryType(Item.InventoryType.FINGER);
            entity.setQuality(Item.Quality.EPIC);

            // when
            Item result = itemMapper.mapToDomain(entity);

            // then
            assertInstanceOf(EquippableItem.class, result);
            assertEquals("Band of Accuria", result.getMetadata().name());
        }

        @Test
        void shouldHandleUnknownStatsInDomainMapping() {
            // given
            WeaponEntity entity = new WeaponEntity();
            entity.setPk(new ItemEntity.CompositeKey(1L, 0L));
            entity.setStats(null);

            // when
            Item result = itemMapper.mapToDomain(entity);

            // then
            assertNotNull(result.getMetadata().stats(), "Stats map should be empty, not null");
            assertTrue(result.getMetadata().stats().isEmpty());
        }

        @Test
        void shouldDefaultNullLevelsToZeroInDomain() {
            // given
            ArmorEntity entity = new ArmorEntity();
            entity.setPk(new ItemEntity.CompositeKey(1L, 0L));
            entity.setName("Broken Boots");
            entity.setItemLevel(null);
            entity.setRequiredLevel(null);
            entity.setInventoryType(Item.InventoryType.FEET);
            entity.setQuality(Item.Quality.COMMON);

            // when
            Item result = itemMapper.mapToDomain(entity);

            // then
            assertEquals(0, result.getMetadata().itemLevel(), "Null Entity level should be 0 in Domain");
            assertEquals(0, result.getMetadata().requiredLevel());
        }
    }

    @Nested
    class SuffixIdExtraction {

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
            // given
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

            // when
            long resultNullEnchs = itemMapper.mapSuffixId(nullEnchs);
            long resultNullName = itemMapper.mapSuffixId(nullName);

            // then
            assertEquals(0L, resultNullEnchs);
            assertEquals(0L, resultNullName);
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
}