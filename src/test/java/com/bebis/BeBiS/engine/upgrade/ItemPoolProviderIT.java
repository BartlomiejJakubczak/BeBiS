package com.bebis.BeBiS.engine.upgrade;

import com.bebis.BeBiS.base.BaseNonTransactionalFullstackTest;
import com.bebis.BeBiS.equipment.domain.Equipment;
import com.bebis.BeBiS.item.domain.Armor;
import com.bebis.BeBiS.item.domain.Item;
import com.bebis.BeBiS.item.domain.Weapon;
import com.bebis.BeBiS.item.jpa.ArmorEntity;
import com.bebis.BeBiS.item.jpa.EquippableItemEntity;
import com.bebis.BeBiS.item.jpa.ItemEntity;
import com.bebis.BeBiS.item.jpa.WeaponEntity;
import com.bebis.BeBiS.profile.domain.WowCharacter;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;


@TestInstance(TestInstance.Lifecycle.PER_CLASS) // by default junit creates a new instance of test class per test
// this forces it to create only one, hence @BeforeAll method no longer has to be static
public class ItemPoolProviderIT extends BaseNonTransactionalFullstackTest {

    @Autowired
    private ItemPoolProvider itemPoolProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @BeforeAll
    void setUp() {
        transactionTemplate.executeWithoutResult(s -> setUpItemsInDb(List.of(
                // armor
                createArmorEntity(1L, 39, Armor.ArmorType.LEATHER),
                createArmorEntity(2L, 39, Armor.ArmorType.MAIL),
                createArmorEntity(3L, 40, Armor.ArmorType.PLATE),
                createArmorEntity(4L, 55, Armor.ArmorType.PLATE),
                // weapons
                createWeaponEntity(5L, 55, Item.InventoryType.RANGED, Weapon.WeaponType.GUN),
                createWeaponEntity(6L, 55, Item.InventoryType.RANGED, Weapon.WeaponType.BOW),
                createWeaponEntity(7L, 55, Item.InventoryType.RANGED, Weapon.WeaponType.WAND),
                // rings + trinkets
                createEquippableEntity(8L, 50, Item.InventoryType.FINGER),
                createEquippableEntity(9L, 50, Item.InventoryType.FINGER),
                createEquippableEntity(10L, 50, Item.InventoryType.FINGER),
                createEquippableEntity(11L, 50, Item.InventoryType.TRINKET),
                createEquippableEntity(12L, 50, Item.InventoryType.TRINKET),
                createEquippableEntity(13L, 50, Item.InventoryType.TRINKET)
        )));
    }

    @AfterAll
    void dbTeardown() {
        // no @Transactional, so manual teardown is needed
        jdbcTemplate.execute("TRUNCATE TABLE items CASCADE"); // postgre-specific syntax
    }

    @ParameterizedTest
    @MethodSource("slotsToExpectedItemPoolProvider")
    void shouldGetTheSameItemPoolForRingsAndTrinketsNoMatterWhichSlot(Set<Equipment.Slot> slots, Set<Long> expectedIds) {
        slots.forEach(slot -> {
            List<Item> result = itemPoolProvider.getItemPool(slot, 60, WowCharacter.WowClass.WARRIOR);
            assertThat(extractIdsFromResult(result)).isEqualTo(expectedIds);
        });
    }

    private static Stream<Arguments> slotsToExpectedItemPoolProvider() {
        return Stream.of(
                Arguments.of(Set.of(Equipment.Slot.FINGER_1, Equipment.Slot.FINGER_2), Set.of(8L, 9L, 10L)),
                Arguments.of(Set.of(Equipment.Slot.TRINKET_1, Equipment.Slot.TRINKET_2), Set.of(11L, 12L, 13L))
        );
    }

    @Test
    void shouldExcludeItemsNotTrainableForAGivenClass() {
        // given / when
        List<Item> result = itemPoolProvider.getItemPool(Equipment.Slot.RANGED, 60, WowCharacter.WowClass.WARRIOR);

        // then
        assertThat(extractIdsFromResult(result)).containsExactlyInAnyOrder(5L, 6L);
    }

    @Test
    void shouldExcludeItemWithRequiredLevelHigherThanCharacterLevel() {
        // given
        WowCharacter.WowClass wowClass = WowCharacter.WowClass.WARRIOR;
        int level = 45;

        // when
        List<Item> result = itemPoolProvider.getItemPool(Equipment.Slot.CHEST, level, wowClass);

        // then
        assertThat(extractIdsFromResult(result)).containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @ParameterizedTest
    @MethodSource("classAndLevelProvider")
    void shouldProvideCorrectItemsEligibleForHybridClassesAtTransitionalLevels(WowCharacter.WowClass wowClass, int level, Set<Long> expectedIds) {
        // given / when
        List<Item> result = itemPoolProvider.getItemPool(Equipment.Slot.CHEST, level, wowClass);

        // then
        assertThat(expectedIds).containsExactlyInAnyOrderElementsOf(extractIdsFromResult(result));
    }

    private Set<Long> extractIdsFromResult(List<Item> result) {
        return result.stream()
                .map((item) -> item.getMetadata().key().baseId())
                .collect(Collectors.toSet());
    }

    private static Stream<Arguments> classAndLevelProvider() {
        return Stream.of(
                Arguments.of(WowCharacter.WowClass.WARRIOR, 39, Set.of(1L, 2L)),
                Arguments.of(WowCharacter.WowClass.WARRIOR, 40, Set.of(1L, 2L, 3L)),
                Arguments.of(WowCharacter.WowClass.PALADIN, 39, Set.of(1L, 2L)),
                Arguments.of(WowCharacter.WowClass.PALADIN, 40, Set.of(1L, 2L, 3L)),
                Arguments.of(WowCharacter.WowClass.HUNTER, 39, Set.of(1L)),
                Arguments.of(WowCharacter.WowClass.HUNTER, 40, Set.of(1L, 2L)),
                Arguments.of(WowCharacter.WowClass.SHAMAN, 39, Set.of(1L)),
                Arguments.of(WowCharacter.WowClass.SHAMAN, 40, Set.of(1L, 2L))
        );
    }

    private void setUpItemsInDb(List<ItemEntity> itemEntities) {
        itemEntities.forEach(entityManager::persist);
        entityManager.flush();
        entityManager.clear();
    }

    private static void setEntityMetadata(ItemEntity entity, long baseId, int requiredLevel, Item.InventoryType inventoryType) {
        entity.setPk(new ItemEntity.CompositeKey(baseId, 0L)); // suffix is irrelevant to this suite
        entity.setName("Placeholder");
        entity.setQuality(Item.Quality.UNCOMMON);
        entity.setInventoryType(inventoryType);
        entity.setItemLevel(21);
        entity.setRequiredLevel(requiredLevel);
        entity.setUniqueEquipped(false);
        entity.setStats(Map.of()); // stats are irrelevant to this suite
        entity.setSpecialEffects(List.of()); // effects are irrelevant to this suite
    }

    private static ArmorEntity createArmorEntity(long baseId, int requiredLevel, Armor.ArmorType armorType) {
        ArmorEntity armorEntity = new ArmorEntity();
        setEntityMetadata(armorEntity, baseId, requiredLevel, Item.InventoryType.CHEST);

        armorEntity.setArmorType(armorType);

        return armorEntity;
    }

    private static WeaponEntity createWeaponEntity(long baseId, int requiredLevel, Item.InventoryType inventoryType, Weapon.WeaponType weaponType) {
        WeaponEntity weaponEntity = new WeaponEntity();
        setEntityMetadata(weaponEntity, baseId, requiredLevel, inventoryType);

        weaponEntity.setWeaponType(weaponType);

        return weaponEntity;
    }

    private static EquippableItemEntity createEquippableEntity(long baseId, int requiredLevel, Item.InventoryType inventoryType) {
        EquippableItemEntity entity = new EquippableItemEntity();
        setEntityMetadata(entity, baseId, requiredLevel, inventoryType);
        return entity;
    }
}
