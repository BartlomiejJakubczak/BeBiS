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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;


@TestInstance(TestInstance.Lifecycle.PER_CLASS) // by default junit creates a new instance of test class per test
// this forces it to create only one, hence @BeforeAll method no longer has to be static
public class ItemPoolProviderIT extends BaseNonTransactionalFullstackTest {

    private static final long HORDE_QUEST_REWARD = 900001L;
    private static final long MIRRORED_QUEST_REWARD = 900002L; // rewarded by an alliance and a horde version of a quest
    private static final long WARRIOR_QUEST_REWARD = 900003L;
    private static final int ALLIANCE_RACE_MASK = 77; // human, dwarf, night elf, gnome
    private static final int HORDE_RACE_MASK = 178; // orc, undead, tauren, troll

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
                createEquippableEntity(13L, 50, Item.InventoryType.TRINKET),
                // no level requirement
                createArmorEntity(14L, 0, Armor.ArmorType.MAIL),
                // class restricted: tier chest wearable by every plate class, but restricted to warriors
                createClassRestrictedArmorEntity(15L, 60, Armor.ArmorType.PLATE, WowCharacter.WowClass.WARRIOR),
                // quest rewards (leg slot, so the chest suites above stay untouched)
                createLegsEntity(HORDE_QUEST_REWARD),
                createLegsEntity(MIRRORED_QUEST_REWARD),
                createLegsEntity(WARRIOR_QUEST_REWARD)
        )));
        // quest_rewards is reference data with no FK to items: ids far above real item ids keep the seed untouched
        insertQuestReward(HORDE_QUEST_REWARD, 900101L, 0, HORDE_RACE_MASK);
        insertQuestReward(MIRRORED_QUEST_REWARD, 900102L, 0, ALLIANCE_RACE_MASK);
        insertQuestReward(MIRRORED_QUEST_REWARD, 900103L, 0, HORDE_RACE_MASK);
        insertQuestReward(WARRIOR_QUEST_REWARD, 900104L, 1, 0); // class bit 1 = warrior
    }

    @ParameterizedTest
    @MethodSource("questRaceRestrictionProvider")
    void shouldOfferQuestRewardOnlyToRacesThatCanTakeAnyOfItsQuests(WowCharacter.Race race, WowCharacter.WowClass wowClass,
                                                                  Set<Long> expectedLegs) {
        // given / when
        List<Item> result = itemPoolProvider.getItemPool(Equipment.Slot.LEGS, 60, race, wowClass).items();

        // then
        assertThat(extractIdsFromResult(result)).isEqualTo(expectedLegs);
    }

    private static Stream<Arguments> questRaceRestrictionProvider() {
        return Stream.of(
                // horde-only quest: hidden from alliance; mirrored quests: one of them always fits
                Arguments.of(WowCharacter.Race.ORC, WowCharacter.WowClass.WARRIOR,
                        Set.of(HORDE_QUEST_REWARD, MIRRORED_QUEST_REWARD, WARRIOR_QUEST_REWARD)),
                Arguments.of(WowCharacter.Race.HUMAN, WowCharacter.WowClass.WARRIOR,
                        Set.of(MIRRORED_QUEST_REWARD, WARRIOR_QUEST_REWARD))
        );
    }

    @Test
    void shouldHideQuestRewardFromClassesTheQuestExcludes_EvenWhenTheItemItselfIsNotClassRestricted() {
        // given / when
        List<Item> result = itemPoolProvider.getItemPool(Equipment.Slot.LEGS, 60, WowCharacter.Race.HUMAN, WowCharacter.WowClass.PALADIN).items();

        // then
        assertThat(extractIdsFromResult(result)).doesNotContain(WARRIOR_QUEST_REWARD);
    }

    @ParameterizedTest
    @MethodSource("classRestrictionProvider")
    void shouldOfferClassRestrictedItemOnlyToItsClasses_EvenWhenOtherClassCanWearItsArmorType(WowCharacter.WowClass wowClass,
                                                                                            boolean expectedInPool) {
        // given / when
        List<Item> result = poolFor(Equipment.Slot.CHEST, 60, wowClass);

        // then
        assertThat(extractIdsFromResult(result).contains(15L)).isEqualTo(expectedInPool);
    }

    private static Stream<Arguments> classRestrictionProvider() {
        return Stream.of(
                Arguments.of(WowCharacter.WowClass.WARRIOR, true),
                Arguments.of(WowCharacter.WowClass.PALADIN, false) // plate at 40+, but not a warrior
        );
    }

    @AfterAll
    void dbTeardown() {
        // no @Transactional, so manual teardown is needed
        jdbcTemplate.execute("TRUNCATE TABLE items CASCADE"); // postgre-specific syntax
        jdbcTemplate.update("DELETE FROM quest_rewards WHERE quest_id >= 900000");
    }

    @ParameterizedTest
    @MethodSource("slotsToExpectedItemPoolProvider")
    void shouldGetTheSameItemPoolForRingsAndTrinketsNoMatterWhichSlot(Set<Equipment.Slot> slots, Set<Long> expectedIds) {
        slots.forEach(slot -> {
            List<Item> result = poolFor(slot, 60, WowCharacter.WowClass.WARRIOR);
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
        List<Item> result = poolFor(Equipment.Slot.RANGED, 60, WowCharacter.WowClass.WARRIOR);

        // then
        assertThat(extractIdsFromResult(result)).containsExactlyInAnyOrder(5L, 6L);
    }

    @Test
    void shouldExcludeItemWithRequiredLevelHigherThanCharacterLevel() {
        // given
        WowCharacter.WowClass wowClass = WowCharacter.WowClass.WARRIOR;
        int level = 45;

        // when
        List<Item> result = poolFor(Equipment.Slot.CHEST, level, wowClass);

        // then
        assertThat(extractIdsFromResult(result)).containsExactlyInAnyOrder(1L, 2L, 3L, 14L);
    }

    @Test
    void shouldIncludeItemWithoutRequiredLevel_WhenCharacterIsLevelOne() {
        // given / when
        List<Item> result = poolFor(Equipment.Slot.CHEST, 1, WowCharacter.WowClass.WARRIOR);

        // then
        assertThat(extractIdsFromResult(result)).containsExactly(14L);
    }

    @ParameterizedTest
    @MethodSource("classAndLevelProvider")
    void shouldProvideCorrectItemsEligibleForHybridClassesAtTransitionalLevels(WowCharacter.WowClass wowClass, int level, Set<Long> expectedIds) {
        // given / when
        List<Item> result = poolFor(Equipment.Slot.CHEST, level, wowClass);

        // then
        assertThat(expectedIds).containsExactlyInAnyOrderElementsOf(extractIdsFromResult(result));
    }

    // the race doesn't matter for items without quest sources, but should still be one the class can have
    private List<Item> poolFor(Equipment.Slot slot, int level, WowCharacter.WowClass wowClass) {
        WowCharacter.Race race = switch (wowClass) {
            case SHAMAN -> WowCharacter.Race.ORC;
            case HUNTER -> WowCharacter.Race.NIGHT_ELF;
            case PALADIN -> WowCharacter.Race.DWARF;
            default -> WowCharacter.Race.HUMAN;
        };
        return itemPoolProvider.getItemPool(slot, level, race, wowClass).items();
    }

    private void insertQuestReward(long baseId, long questId, int classMask, int raceMask) {
        jdbcTemplate.update("INSERT INTO quest_rewards (base_id, quest_id, quest_min_level, quest_level, reward_kind, " +
                "required_classes, required_races) VALUES (?, ?, 1, 60, 'CHOICE', ?, ?)", baseId, questId, classMask, raceMask);
    }

    private Set<Long> extractIdsFromResult(List<Item> result) {
        return result.stream()
                .map((item) -> item.getMetadata().key().baseId())
                .collect(Collectors.toSet());
    }

    private static Stream<Arguments> classAndLevelProvider() {
        return Stream.of(
                Arguments.of(WowCharacter.WowClass.WARRIOR, 39, Set.of(1L, 2L, 14L)),
                Arguments.of(WowCharacter.WowClass.WARRIOR, 40, Set.of(1L, 2L, 3L, 14L)),
                Arguments.of(WowCharacter.WowClass.PALADIN, 39, Set.of(1L, 2L, 14L)),
                Arguments.of(WowCharacter.WowClass.PALADIN, 40, Set.of(1L, 2L, 3L, 14L)),
                Arguments.of(WowCharacter.WowClass.HUNTER, 39, Set.of(1L)),
                Arguments.of(WowCharacter.WowClass.HUNTER, 40, Set.of(1L, 2L, 14L)),
                Arguments.of(WowCharacter.WowClass.SHAMAN, 39, Set.of(1L)),
                Arguments.of(WowCharacter.WowClass.SHAMAN, 40, Set.of(1L, 2L, 14L))
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

    private static ArmorEntity createLegsEntity(long baseId) {
        ArmorEntity legs = new ArmorEntity();
        setEntityMetadata(legs, baseId, 50, Item.InventoryType.LEGS);
        legs.setArmorType(Armor.ArmorType.MAIL);
        return legs;
    }

    private static ArmorEntity createClassRestrictedArmorEntity(long baseId, int requiredLevel, Armor.ArmorType armorType,
                                                                WowCharacter.WowClass... playableClasses) {
        ArmorEntity armorEntity = createArmorEntity(baseId, requiredLevel, armorType);
        armorEntity.setPlayableClasses(new HashSet<>(Set.of(playableClasses)));
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
