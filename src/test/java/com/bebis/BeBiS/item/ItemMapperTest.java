package com.bebis.BeBiS.item;

import com.bebis.BeBiS.equipment.EquipmentResponseBuilder;
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
import com.bebis.BeBiS.profile.domain.WowCharacter;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

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
            EquipmentResponse.ItemDTO dto = EquipmentResponseBuilder.newInstance(base)
                    .withSlot("MAIN_HAND")
                    .build();

            // when
            ItemSyncData result = itemMapper.mapToSyncData(base, dto);

            // then
            assertTrue(result.isWeapon());
            assertEquals(base.preview().weapon().attackSpeed().value(), result.weapon().speed());
            assertEquals(base.subclass().name().toUpperCase(), result.weapon().weaponType().name());
            assertEquals(base.preview().weapon().damage().minValue(), result.weapon().minDamage());
            assertEquals(base.preview().weapon().damage().maxValue(), result.weapon().maxDamage());
            assertEquals(base.preview().weapon().dps().value(), result.weapon().dps());
        }

        @Test
        void shouldNormalizeHighWeaponSpeed() {
            // given
            double speedToNormalize = 3600.0;
            double expectedNormalizedSpeed = speedToNormalize / 1000;

            ItemResponse base = ItemResponseBuilder.newWeaponInstance()
                    .withAttackSpeed(3600.0) // speed in thousands
                    .build();
            EquipmentResponse.ItemDTO highSpeedDto = EquipmentResponseBuilder.newInstance(base)
                    .withSlot("MAIN_HAND")
                    .build();

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
            EquipmentResponse.ItemDTO dto = EquipmentResponseBuilder.newInstance(base)
                    .withSlot("CHEST")
                    .build();

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
            EquipmentResponse.ItemDTO dto = EquipmentResponseBuilder.newInstance(base)
                    .withSlot("FINGER_1")
                    .build();

            // when
            ItemSyncData result = itemMapper.mapToSyncData(base, dto);

            // then
            assertTrue(result.isEquippable());
        }

        @Test
        void shouldCaptureArmorOnNonArmorItems() {
            // given
            Integer expectedArmorValue = 150;
            ItemResponse response = ItemResponseBuilder.newSuffixableEquippableInstance()
                    .withArmorValue(expectedArmorValue)
                    .build();
            EquipmentResponse.ItemDTO dto = EquipmentResponseBuilder.newInstance(response)
                    .withSuffix(new EquipmentResponseBuilder.Suffix(37, "of The Monkey"))
                    .withSlot("FINGER_1")
                    .withItemLevel(response.itemLevel() + 10)
                    .withStats(Map.of("AGILITY", 20))
                    .build();

            // when
            ItemSyncData result = itemMapper.mapToSyncData(response, dto);

            // then
            assertEquals(expectedArmorValue, result.commonData().stats().get(StatType.ARMOR), "Should capture armor even on a ring");
        }

        @Test
        void shouldKeepArmorNullWhenMissingInResponse() {
            // given
            ItemResponse base = ItemResponseBuilder.newWeaponInstance().build();
            EquipmentResponse.ItemDTO dto = EquipmentResponseBuilder.newInstance(base)
                    .withSlot("MAIN_HAND")
                    .build();

            // when
            ItemSyncData result = itemMapper.mapToSyncData(base, dto);

            // then
            assertThat(result.commonData().stats().get(StatType.ARMOR)).isNull();
        }

        @Test
        void shouldFallbackToBaseItemArmorWhenEquippedArmorIsNull() {
            // given
            int expectedArmor = 150;
            ItemResponse base = ItemResponseBuilder.newEquippableInstance()
                    .withArmorValue(expectedArmor)
                    .build();
            EquipmentResponse.ItemDTO dto = EquipmentResponseBuilder.newInstance(base)
                    .withSlot("FINGER_1")
                    .build();

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

            ItemResponse base = ItemResponseBuilder.newSuffixableEquippableInstance()
                    .withArmorValue(armorValue)
                    .build();

            EquipmentResponse.ItemDTO dtoWithMixedStats = EquipmentResponseBuilder.newInstance(base)
                    .withSuffix(new EquipmentResponseBuilder.Suffix(37, "of The Monkey"))
                    .withSlot("FINGER_1")
                    .withItemLevel(base.itemLevel() + 10)
                    .withStats(Map.of("AGILITY", agiValue, "WEIRD_BLIZZARD_STAT_99", 100))
                    .build();

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

            EquipmentResponse.ItemDTO dto = EquipmentResponseBuilder.newInstance(base)
                    .withSlot("FINGER_1")
                    .build();

            // when
            ItemSyncData result = itemMapper.mapToSyncData(base, dto);

            // then
            assertThat(result.commonData().uniqueEquipped()).isFalse();
        }

        @Test
        void shouldMapNullRequiredLevelToZero() {
            // given
            ItemResponse base = ItemResponseBuilder.newEquippableInstance()
                    .withRequiredLevel(null)
                    .build();

            EquipmentResponse.ItemDTO dto = EquipmentResponseBuilder.newInstance(base)
                    .withSlot("FINGER_1")
                    .build();

            // when
            ItemSyncData result = itemMapper.mapToSyncData(base, dto);

            // then
            assertThat(result.commonData().requiredLevel()).isZero();
        }

        @Test
        void shouldMapItemWithParametersFromEqDTO_WhenPreviewMissing() {
            // given
            ItemResponse base = ItemResponseBuilder.newSuffixableEquippableInstance().build();
            assertThat(base.preview()).isNull(); // guard: the builder omits preview when all of its fields are empty

            EquipmentResponse.ItemDTO dto = EquipmentResponseBuilder.newInstance(base)
                    .withSuffix(new EquipmentResponseBuilder.Suffix(37, "of The Monkey"))
                    .withSlot("FINGER_1")
                    .withStats(Map.of("AGILITY", 20))
                    .build();

            // when
            ItemSyncData result = itemMapper.mapToSyncData(base, dto);

            // then
            assertThat(result.commonData().uniqueEquipped()).isFalse();
            assertThat(result.commonData().specialEffects()).isEmpty();
            assertThat(result.commonData().stats()).containsExactlyEntriesOf(Map.of(StatType.AGILITY, 20));
        }

        @Test
        void shouldMapPlayableClassesById_WhenClassNameIsLocalized() {
            // given
            ItemResponse base = ItemResponseBuilder.newWeaponInstance()
                    .withPlayableClass(1, "Krieger") // the name depends on the locale, the id doesn't
                    .build();
            EquipmentResponse.ItemDTO dto = EquipmentResponseBuilder.newInstance(base)
                    .withSlot("MAIN_HAND")
                    .build();

            // when
            ItemSyncData result = itemMapper.mapToSyncData(base, dto);

            // then
            assertThat(result.commonData().playableClasses()).containsExactly(WowCharacter.WowClass.WARRIOR);
        }

        @Test
        void shouldFallbackToUnknownForBadEnums() {
            // given
            ItemResponse base = ItemResponseBuilder.newEquippableInstance().build();

            EquipmentResponse.ItemDTO dto = EquipmentResponseBuilder.newInstance(base)
                    .withSlot("FINGER_1")
                    .withQuality("WEIRD_QUALITY")
                    .build();

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
            EquipmentResponse.ItemDTO dto = EquipmentResponseBuilder.newInstance(base)
                    .withSlot("MAIN_HAND")
                    .build();

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
            EquipmentResponse.ItemDTO dto = EquipmentResponseBuilder.newInstance(base)
                    .withSlot("MAIN_HAND")
                    .build();

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
            EquipmentResponse.ItemDTO dto = EquipmentResponseBuilder.newInstance(baseMissingClass)
                    .withSlot("MAIN_HAND")
                    .build();

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
            EquipmentResponse.ItemDTO dto = EquipmentResponseBuilder.newInstance(baseContainer)
                    .withSlot("BAG")
                    .build();

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

            EquipmentResponse.ItemDTO dto1 = EquipmentResponseBuilder.newInstance(baseNullInv)
                    .withSlot("FINGER_1")
                    .build();

            ItemResponse baseBadInv = ItemResponseBuilder.newEquippableInstance()
                    .withInventoryType("WEIRD_TYPE")
                    .build();

            EquipmentResponse.ItemDTO dto2 = EquipmentResponseBuilder.newInstance(baseBadInv)
                    .withSlot("FINGER_1")
                    .build();

            // when / then
            assertThatThrownBy(() -> itemMapper.mapToSyncData(baseNullInv, dto1))
                    .isInstanceOf(InvalidItemException.class)
                    .hasMessage("inventoryType cannot be null for upgrade analysis purposes");

            assertThatThrownBy(() -> itemMapper.mapToSyncData(baseBadInv, dto2))
                    .isInstanceOf(InvalidItemException.class)
                    .hasMessageContaining("Could not map inventory type");
        }

        @Test
        void shouldRejectItem_WhenPlayableClassIdDoesNotExistInClassic() {
            // given
            ItemResponse base = ItemResponseBuilder.newWeaponInstance()
                    .withPlayableClass(6, "Death Knight") // a restriction we can't interpret must not become "usable by all"
                    .build();
            EquipmentResponse.ItemDTO dto = EquipmentResponseBuilder.newInstance(base)
                    .withSlot("MAIN_HAND")
                    .build();

            // when / then
            assertThatThrownBy(() -> itemMapper.mapToSyncData(base, dto))
                    .isInstanceOf(InvalidItemException.class)
                    .hasMessageContaining("Invalid playable class restriction");
        }

        @Test
        void shouldThrowInvalidItemExceptionWhenNameIsMissing() {
            // given
            ItemResponse baseNoName = ItemResponseBuilder.newEquippableInstance()
                    .withName(null)
                    .build();

            EquipmentResponse.ItemDTO dto = EquipmentResponseBuilder.newInstance(baseNoName)
                    .withSlot("FINGER_1")
                    .build();

            // when / then
            assertThatThrownBy(() -> itemMapper.mapToSyncData(baseNoName, dto))
                    .isInstanceOf(InvalidItemException.class)
                    .hasMessage("Critical data missing: name");
        }

        @Test
        void shouldThrowInvalidItemExceptionWhenWeaponDataIsMissing() {
            // given
            ItemResponse weaponResponse = ItemResponseBuilder.newWeaponInstance().build();

            EquipmentResponse.ItemDTO weaponDTO = EquipmentResponseBuilder.newInstance(weaponResponse)
                    .withAttackSpeed(null)
                    .withDps(null)
                    .withMinDamage(null)
                    .withMaxDamage(null)
                    // all 4 null result in null weaponDTO
                    .build();

            // when / then
            assertThatThrownBy(() -> itemMapper.mapToSyncData(weaponResponse, weaponDTO))
                    .isInstanceOf(InvalidItemException.class)
                    .hasMessage("weapon data cannot be null for weapon upgrade analysis purposes");
        }

        @Test
        void shouldThrowInvalidItemExceptionWhenAttackSpeedDataIsMissing() {
            // given
            ItemResponse weaponResponse = ItemResponseBuilder.newWeaponInstance().build();

            EquipmentResponse.ItemDTO weaponDTO = EquipmentResponseBuilder.newInstance(weaponResponse)
                    .withAttackSpeed(null)
                    .build();

            // when / then
            assertThatThrownBy(() -> itemMapper.mapToSyncData(weaponResponse, weaponDTO))
                    .isInstanceOf(InvalidItemException.class)
                    .hasMessage("weapon speed cannot be null for weapon upgrade analysis purposes");
        }

        @Test
        void shouldThrowInvalidItemExceptionWhenMinDamageDataIsMissing() {
            // given
            ItemResponse weaponResponse = ItemResponseBuilder.newWeaponInstance().build();

            EquipmentResponse.ItemDTO weaponDTO = EquipmentResponseBuilder.newInstance(weaponResponse)
                    .withMinDamage(null)
                    .build();

            // when / then
            assertThatThrownBy(() -> itemMapper.mapToSyncData(weaponResponse, weaponDTO))
                    .isInstanceOf(InvalidItemException.class)
                    .hasMessage("weapon damage cannot be null for weapon upgrade analysis purposes");
        }

        @Test
        void shouldThrowInvalidItemExceptionWhenMaxDamageDataIsMissing() {
            // given
            ItemResponse weaponResponse = ItemResponseBuilder.newWeaponInstance().build();

            EquipmentResponse.ItemDTO weaponDTO = EquipmentResponseBuilder.newInstance(weaponResponse)
                    .withMaxDamage(null)
                    .build();

            // when / then
            assertThatThrownBy(() -> itemMapper.mapToSyncData(weaponResponse, weaponDTO))
                    .isInstanceOf(InvalidItemException.class)
                    .hasMessage("weapon damage cannot be null for weapon upgrade analysis purposes");
        }

        @Test
        void shouldThrowInvalidItemExceptionWhenDpsDataIsMissing() {
            // given
            ItemResponse weaponResponse = ItemResponseBuilder.newWeaponInstance().build();

            EquipmentResponse.ItemDTO weaponDTO = EquipmentResponseBuilder.newInstance(weaponResponse)
                    .withDps(null)
                    .build();

            // when / then
            assertThatThrownBy(() -> itemMapper.mapToSyncData(weaponResponse, weaponDTO))
                    .isInstanceOf(InvalidItemException.class)
                    .hasMessage("weapon dps cannot be null for weapon upgrade analysis purposes");
        }
    }

    @Nested
    class InventoryTypeMapping {

        @ParameterizedTest
        @MethodSource("blizzardCodeToInventoryType")
        void shouldMapBlizzardInventoryTypeCode_ToDomainInventoryType(String blizzardCode, Item.InventoryType expected) {
            // given
            ItemResponse base = ItemResponseBuilder.newEquippableInstance()
                    .withInventoryType(blizzardCode)
                    .build();
            EquipmentResponse.ItemDTO dto = EquipmentResponseBuilder.newInstance(base)
                    .withSlot("FINGER_1")
                    .build();

            // when
            ItemSyncData result = itemMapper.mapToSyncData(base, dto);

            // then
            assertThat(result.commonData().inventoryType()).isEqualTo(expected);
        }

        static Stream<Arguments> blizzardCodeToInventoryType() {
            return Stream.of(
                    Arguments.of("HEAD", Item.InventoryType.HEAD),
                    Arguments.of("NECK", Item.InventoryType.NECK),
                    Arguments.of("SHOULDER", Item.InventoryType.SHOULDER),
                    Arguments.of("BODY", Item.InventoryType.BODY),
                    Arguments.of("CHEST", Item.InventoryType.CHEST),
                    Arguments.of("ROBE", Item.InventoryType.CHEST),
                    Arguments.of("WAIST", Item.InventoryType.WAIST),
                    Arguments.of("LEGS", Item.InventoryType.LEGS),
                    Arguments.of("FEET", Item.InventoryType.FEET),
                    Arguments.of("WRIST", Item.InventoryType.WRIST),
                    Arguments.of("HAND", Item.InventoryType.HANDS),
                    Arguments.of("FINGER", Item.InventoryType.FINGER),
                    Arguments.of("TRINKET", Item.InventoryType.TRINKET),
                    Arguments.of("CLOAK", Item.InventoryType.CLOAK),
                    Arguments.of("BACK", Item.InventoryType.CLOAK),
                    Arguments.of("TABARD", Item.InventoryType.TABARD),
                    Arguments.of("WEAPON", Item.InventoryType.WEAPON),
                    Arguments.of("TWOHWEAPON", Item.InventoryType.TWO_HAND),
                    Arguments.of("WEAPONMAINHAND", Item.InventoryType.WEAPONMAINHAND),
                    Arguments.of("WEAPONOFFHAND", Item.InventoryType.WEAPONOFFHAND),
                    Arguments.of("SHIELD", Item.InventoryType.SHIELD),
                    Arguments.of("HOLDABLE", Item.InventoryType.HOLDABLE),
                    Arguments.of("RANGED", Item.InventoryType.RANGED),
                    Arguments.of("RANGEDRIGHT", Item.InventoryType.RANGEDRIGHT),
                    Arguments.of("THROWN", Item.InventoryType.THROWN),
                    Arguments.of("RELIC", Item.InventoryType.RELIC)
            );
        }

        @ParameterizedTest
        @ValueSource(strings = {"NON_EQUIP", "BAG", "AMMO", "QUIVER", "TWO_HAND", "HANDS"})
        void shouldRejectInventoryType_WhenCodeIsNotEquippableGearOrNotABlizzardCode(String blizzardCode) {
            // given
            ItemResponse base = ItemResponseBuilder.newEquippableInstance()
                    .withInventoryType(blizzardCode)
                    .build();
            EquipmentResponse.ItemDTO dto = EquipmentResponseBuilder.newInstance(base)
                    .withSlot("FINGER_1")
                    .build();

            // when / then
            assertThatThrownBy(() -> itemMapper.mapToSyncData(base, dto))
                    .isInstanceOf(InvalidItemException.class)
                    .hasMessageContaining("Could not map inventory type");
        }
    }

    /**
     * Contract tests against responses recorded from the real Blizzard API (src/test/resources/blizzard/item).
     * Builders encode our assumptions about the API, these fixtures encode what it actually sends.
     */
    @Nested
    class RecordedBlizzardResponses {

        @ParameterizedTest
        @MethodSource("recordedItems")
        void shouldMapRecordedResponse_ToInventoryTypeLevelAndUniqueness(String fixture, String slot,
                                                                          Item.InventoryType expectedType,
                                                                          int expectedRequiredLevel,
                                                                          boolean expectedUnique) {
            // given
            ItemResponse base = BlizzardFixtures.loadItem(fixture);
            EquipmentResponse.ItemDTO dto = EquipmentResponseBuilder.newInstance(base)
                    .withSlot(slot)
                    .build();

            // when
            ItemSyncData result = itemMapper.mapToSyncData(base, dto);

            // then
            assertThat(result.commonData().baseId()).isEqualTo(base.id());
            assertThat(result.commonData().inventoryType()).isEqualTo(expectedType);
            assertThat(result.commonData().requiredLevel()).isEqualTo(expectedRequiredLevel);
            assertThat(result.commonData().uniqueEquipped()).isEqualTo(expectedUnique);
        }

        static Stream<Arguments> recordedItems() {
            return Stream.of(
                    // Whirlwind Axe: quest reward, no level requirement on the item itself
                    Arguments.of("two_hand.json", "MAIN_HAND", Item.InventoryType.TWO_HAND, 0, false),
                    // Sword of Omen: quest reward, no level requirement on the item itself
                    Arguments.of("one_hand.json", "MAIN_HAND", Item.InventoryType.WEAPON, 0, false),
                    Arguments.of("neck.json", "NECK", Item.InventoryType.NECK, 39, false),
                    Arguments.of("ring.json", "FINGER_1", Item.InventoryType.FINGER, 60, true),
                    Arguments.of("trinket.json", "TRINKET_1", Item.InventoryType.TRINKET, 53, true)
            );
        }

        @Test
        void shouldMapRecordedTwoHandWeapon_WithSpeedNormalizedFromMilliseconds() {
            // given
            ItemResponse base = BlizzardFixtures.loadItem("two_hand.json"); // attack_speed comes as 3600
            EquipmentResponse.ItemDTO dto = EquipmentResponseBuilder.newInstance(base)
                    .withSlot("MAIN_HAND")
                    .build();

            // when
            ItemSyncData result = itemMapper.mapToSyncData(base, dto);

            // then
            assertThat(result.isWeapon()).isTrue();
            assertThat(result.weapon().weaponType()).isEqualTo(Weapon.WeaponType.AXE);
            assertThat(result.weapon().speed()).isEqualTo(3.6);
            assertThat(result.weapon().minDamage()).isEqualTo(102);
            assertThat(result.weapon().maxDamage()).isEqualTo(154);
        }

        @Test
        void shouldRestrictItemToListedClasses_WhenRecordedResponseHasPlayableClasses() {
            // given
            ItemResponse base = BlizzardFixtures.loadItem("two_hand.json"); // Whirlwind Axe: "Classes: Warrior"
            EquipmentResponse.ItemDTO dto = EquipmentResponseBuilder.newInstance(base)
                    .withSlot("MAIN_HAND")
                    .build();

            // when
            ItemSyncData result = itemMapper.mapToSyncData(base, dto);

            // then
            assertThat(result.commonData().playableClasses()).containsExactly(WowCharacter.WowClass.WARRIOR);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "one_hand.json", // no requirements at all
                "neck.json"      // requirements with a level only, no playable_classes
        })
        void shouldNotRestrictClasses_WhenRecordedResponseHasNoPlayableClasses(String fixture) {
            // given
            ItemResponse base = BlizzardFixtures.loadItem(fixture);
            EquipmentResponse.ItemDTO dto = EquipmentResponseBuilder.newInstance(base)
                    .withSlot("MAIN_HAND")
                    .build();

            // when
            ItemSyncData result = itemMapper.mapToSyncData(base, dto);

            // then
            assertThat(result.commonData().playableClasses()).isEmpty();
        }

        @Test
        void shouldKeepEquipEffectsOnlyAsDescriptions_WhenStatsComeAsSpells() {
            // given
            ItemResponse base = BlizzardFixtures.loadItem("trinket.json"); // Hand of Justice: +20 AP is a spell, not a stat
            EquipmentResponse.ItemDTO dto = EquipmentResponseBuilder.newInstance(base)
                    .withSlot("TRINKET_1")
                    .build();

            // when
            ItemSyncData result = itemMapper.mapToSyncData(base, dto);

            // then
            assertThat(result.commonData().stats()).doesNotContainKey(StatType.ATTACK_POWER);
            assertThat(result.commonData().specialEffects()).containsExactlyInAnyOrder(
                    "Equip: 2% chance on melee hit to gain 1 extra attack.",
                    "Equip: +20 Attack Power.");
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

        @ParameterizedTest
        @MethodSource("entityPlayableClasses")
        void shouldMapEntityPlayableClassesToDomain(Set<WowCharacter.WowClass> playableClasses) {
            // given
            EquippableItemEntity entity = new EquippableItemEntity();
            entity.setPk(new ItemEntity.CompositeKey(16865L, 0L));
            entity.setInventoryType(Item.InventoryType.CHEST);
            entity.setPlayableClasses(new HashSet<>(playableClasses));

            // when
            Item result = itemMapper.mapToDomain(entity);

            // then
            assertThat(result.getMetadata().playableClasses()).containsExactlyInAnyOrderElementsOf(playableClasses);
        }

        static Stream<Arguments> entityPlayableClasses() {
            return Stream.of(
                    Arguments.of(Set.of()), // the common case: no class restriction
                    Arguments.of(Set.of(WowCharacter.WowClass.WARRIOR)),
                    Arguments.of(Set.of(WowCharacter.WowClass.MAGE, WowCharacter.WowClass.WARLOCK))
            );
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

            ItemResponse base = ItemResponseBuilder.newSuffixableArmorInstance()
                    .withName("Bracers")
                    .build();

            EquipmentResponse.ItemDTO dto = EquipmentResponseBuilder.newInstance(base)
                    .withSuffix(new EquipmentResponseBuilder.Suffix(suffixId, "of the Whale"))
                    .withSlot("WRISTS")
                    .build();

            // when
            long result = itemMapper.mapSuffixId(dto);

            // then
            assertEquals(suffixId, result, "Should extract the ID when name ends with suffix and starts with 'of '");
        }

        @Test
        void shouldReturnZeroWhenEnchantmentDoesNotStartWithOf() {
            // given
            ItemResponse base = ItemResponseBuilder.newArmorInstance().build();

            EquipmentResponse.ItemDTO dto = EquipmentResponseBuilder.newInstance(base)
                    .withSlot("WRISTS")
                    .withEnchantments(Map.of(123L, "+9 Stamina"))
                    .build();

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
            EquipmentResponse.ItemDTO dto = EquipmentResponseBuilder.newInstance(base)
                    .withSlot("WRISTS")
                    .withEnchantments(Map.of(123L, "+of the Tiger"))
                    .build();

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

            ItemResponse base = ItemResponseBuilder.newSuffixableArmorInstance()
                    .withName("Bracers")
                    .build();

            EquipmentResponse.ItemDTO dto = EquipmentResponseBuilder.newInstance(base)
                    .withSuffix(new EquipmentResponseBuilder.Suffix(suffixId, "of the Bear"))
                    .withSlot("WRISTS")
                    .withEnchantments(Map.of(2137L, "Crusader"))
                    .build();

            // when
            long result = itemMapper.mapSuffixId(dto);

            // then
            assertEquals(suffixId, result, "Should correctly pick 'of the Bear' and ignore 'Crusader'");
        }
    }
}