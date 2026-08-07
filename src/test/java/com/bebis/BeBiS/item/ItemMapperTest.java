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
import org.junit.jupiter.api.Disabled;
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
            ItemResponse base = ItemResponseBuilder.newWeaponInstance().build();
            EquipmentResponse.ItemDTO dto = EquipmentTestData.fromItemResponseNoSuffix(base, "MAIN_HAND", List.of());

            // when
            ItemSyncData result = itemMapper.mapToSyncData(base, dto);

            // then
            assertTrue(result.isWeapon());
            assertEquals(base.preview().weapon().attackSpeed().value(), result.weapon().speed());
            assertEquals(base.subclass().name().toUpperCase(), result.weapon().weaponType().name());
        }

        @Test
        void shouldNormalizeHighWeaponSpeed() {
            // given
            double speedToNormalize = 3600.0;
            double expectedNormalizedSpeed = speedToNormalize / 1000;

            ItemResponse base = ItemResponseBuilder.newWeaponInstance()
                    .withAttackSpeed(3600.0) // speed in thousands
                    .build();
            EquipmentResponse.ItemDTO highSpeedDto = EquipmentTestData.fromItemResponseNoSuffix(base, "MAIN_HAND", List.of());

            // when
            ItemSyncData result = itemMapper.mapToSyncData(base, highSpeedDto);

            // then
            assertEquals(expectedNormalizedSpeed, result.weapon().speed(), "Should divide by 1000 if speed is > 100");
        }

        @Test
        void shouldMapArmorSyncDataCorrectly() {
            // given
            ItemResponse base = ItemResponseBuilder.newArmorInstance()
                    .withSubClass(new ItemResponseBuilder.SubClass(4, "PLATE"))
                    .build();
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
            ItemResponse base = ItemResponseBuilder.newEquippableInstance().build();
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
            ItemResponse response = ItemResponseBuilder.newEquippableInstance()
                    .withArmorValue(expectedArmorValue)
                    .build();
            EquipmentResponse.ItemDTO dto = EquipmentTestData.fromItemResponseSuffixed(
                    response, "FINGER_1", response.quality().type().toUpperCase(), "of The Monkey",
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
            ItemResponse base = ItemResponseBuilder.newWeaponInstance().build();
            EquipmentResponse.ItemDTO dto = EquipmentTestData.fromItemResponseNoSuffix(base, "MAIN_HAND", List.of());

            // when
            ItemSyncData result = itemMapper.mapToSyncData(base, dto);

            // then
            assertThat(result.commonData().stats().get(StatType.ARMOR)).isNull();
        }

        @Disabled
        // TODO this test is broken and should be fixed by test data refactor (atm fromItemResponseNoSuffix just takes armor from base)
        @Test
        void shouldFallbackToBaseItemArmorWhenEquippedArmorIsNull() {
            // given
            int expectedArmor = 150;
            ItemResponse base = ItemResponseBuilder.newEquippableInstance()
                    .withArmorValue(expectedArmor)
                    .build();
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
            ItemResponse base = ItemResponseBuilder.newEquippableInstance()
                    .withArmorValue(armorValue)
                    .build();
            EquipmentResponse.ItemDTO dtoWithMixedStats = EquipmentTestData.fromItemResponseSuffixed(
                    base, "FINGER_1", base.quality().type().toUpperCase(), "of The Monkey", 37L, base.itemLevel() + 10,
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
            ItemResponse base = ItemResponseBuilder.newEquippableInstance()
                    .withUniqueEquipped(null)
                    .build();
            EquipmentResponse.ItemDTO dto = EquipmentTestData.fromItemResponseNoSuffix(base, "FINGER_1", List.of());

            // when
            ItemSyncData result = itemMapper.mapToSyncData(base, dto);

            // then
            assertThat(result.commonData().uniqueEquipped()).isFalse();
        }

        @Test
        void shouldFallbackToUnknownForBadEnums() {
            // given
            ItemResponse base = ItemResponseBuilder.newEquippableInstance()
                    .withQuality("WERID_QUALITY")
                    .build();
            EquipmentResponse.ItemDTO dto = EquipmentTestData.fromItemResponseNoSuffix(base, "FINGER_1", List.of());

            // when
            ItemSyncData result = itemMapper.mapToSyncData(base, dto);

            // then
            assertEquals(Item.Quality.UNKNOWN, result.commonData().quality());
        }

        @Test
        void shouldMapSpecialEffectsCorrectly() {
            // given
            String weaponEffect = "Cool effect";

            ItemResponse base = ItemResponseBuilder.newWeaponInstance()
                    .withSpells(List.of(weaponEffect))
                    .build();
            EquipmentResponse.ItemDTO dto = EquipmentTestData.fromItemResponseNoSuffix(base, "MAIN_HAND", List.of());

            // when
            ItemSyncData result = itemMapper.mapToSyncData(base, dto);

            // then
            assertThat(result.commonData().specialEffects()).containsExactly(weaponEffect);
        }
    }

    @Nested
    class SyncDataValidation {

        @Test
        void shouldThrowExceptionWhenTopLevelInputsAreNull() {
            // given
            ItemResponse base = ItemResponseBuilder.newWeaponInstance().build();
            EquipmentResponse.ItemDTO dto = EquipmentTestData.fromItemResponseNoSuffix(base, "MAIN_HAND", List.of());

            // when / then
            assertThatThrownBy(() -> itemMapper.mapToSyncData(null, dto))
                    .isInstanceOf(InvalidItemException.class)
                    .hasMessage("item and equippedItem responses cannot be null for upgrade analysis purposes");

            assertThatThrownBy(() -> itemMapper.mapToSyncData(base, null))
                    .isInstanceOf(InvalidItemException.class)
                    .hasMessage("item and equippedItem responses cannot be null for upgrade analysis purposes");
        }

        @Test
        void shouldThrowExceptionWhenClassOrSubclassIsNull() {
            // given
            ItemResponse baseMissingClass = ItemResponseBuilder.newEquippableInstance()
                    .withItemClass(null)
                    .withSubClass(null)
                    .build();
            EquipmentResponse.ItemDTO dto = EquipmentTestData.fromItemResponseNoSuffix(baseMissingClass, "MAIN_HAND", List.of());

            // when / then
            assertThatThrownBy(() -> itemMapper.mapToSyncData(baseMissingClass, dto))
                    .isInstanceOf(InvalidItemException.class)
                    .hasMessage("classId and subclassId cannot be null for upgrade analysis purposes");
        }

        @Test
        void shouldThrowExceptionForUnsupportedClassId() {
            // given
            ItemResponseBuilder.ItemClass notSupportedItemClass = new ItemResponseBuilder.ItemClass(1, "Bag");

            ItemResponse baseContainer = ItemResponseBuilder.newEquippableInstance()
                    .withItemClass(notSupportedItemClass)
                    .build();
            EquipmentResponse.ItemDTO dto = EquipmentTestData.fromItemResponseNoSuffix(baseContainer, "BAG", List.of());

            // when / then
            assertThatThrownBy(() -> itemMapper.mapToSyncData(baseContainer, dto))
                    .isInstanceOf(InvalidItemException.class)
                    .hasMessage("Invalid classId: " + notSupportedItemClass.id());
        }

        @Test
        void shouldThrowExceptionWhenInventoryTypeIsMissingOrInvalid() {
            // given
            ItemResponse baseNullInv = ItemResponseBuilder.newEquippableInstance()
                    .withInventoryType(null)
                    .build();
            EquipmentResponse.ItemDTO dto1 = EquipmentTestData.fromItemResponseNoSuffix(baseNullInv, "FINGER_1", List.of());

            ItemResponse baseBadInv = ItemResponseBuilder.newEquippableInstance()
                    .withInventoryType("WEIRD_TYPE")
                    .build();
            EquipmentResponse.ItemDTO dto2 = EquipmentTestData.fromItemResponseNoSuffix(baseBadInv, "FINGER_1", List.of());

            // when / then
            assertThatThrownBy(() -> itemMapper.mapToSyncData(baseNullInv, dto1))
                    .isInstanceOf(InvalidItemException.class)
                    .hasMessage("inventoryType cannot be null for upgrade analysis purposes");

            assertThatThrownBy(() -> itemMapper.mapToSyncData(baseBadInv, dto2))
                    .isInstanceOf(InvalidItemException.class)
                    .hasMessageContaining("Could not map inventory type");
        }

        @Test
        void shouldThrowInvalidItemExceptionWhenNameIsMissing() {
            // given
            ItemResponse baseNoName = ItemResponseBuilder.newEquippableInstance()
                    .withName(null)
                    .build();
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
        void shouldMapEntityPkToDomainKey() {
            // given
            long baseId = 21L;
            long suffixId = 37L;

            EquippableItemEntity entity = new EquippableItemEntity();
            entity.setPk(new ItemEntity.CompositeKey(baseId, suffixId));

            // when
            Item result = itemMapper.mapToDomain(entity);

            // then
            assertThat(result.getMetadata().key().baseId()).isEqualTo(baseId);
            assertThat(result.getMetadata().key().suffixId()).isEqualTo(suffixId);
        }

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
            ItemResponse base = ItemResponseBuilder.newArmorInstance()
                    .withName("Bracers")
                    .build();
            EquipmentResponse.ItemDTO dto = EquipmentTestData.fromItemResponseSuffixed(
                    base, "WRISTS", "RARE", "of the Whale", suffixId, 20, List.of(), List.of()
            );

            // when
            long result = itemMapper.mapSuffixId(dto);

            // then
            assertEquals(suffixId, result, "Should extract the ID when name ends with suffix and starts with 'of '");
        }

        @Test
        void shouldReturnZeroWhenEnchantmentDoesNotStartWithOf() {
            // given
            ItemResponse base = ItemResponseBuilder.newArmorInstance().build();
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
            ItemResponse base = ItemResponseBuilder.newArmorInstance()
                    .withName("Bracers")
                    .build();
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
            ItemResponse base = ItemResponseBuilder.newArmorInstance()
                    .withName("Bracers")
                    .build();
            EquipmentResponse.ItemDTO dto = EquipmentTestData.fromItemResponseSuffixed(
                    base, "WRISTS", "RARE", "of the Bear", suffixId, 20,
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