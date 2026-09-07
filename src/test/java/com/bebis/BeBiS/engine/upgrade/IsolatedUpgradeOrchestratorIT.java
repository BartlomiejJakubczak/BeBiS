package com.bebis.BeBiS.engine.upgrade;

import com.bebis.BeBiS.base.BaseNonTransactionalFullstackTest;
import com.bebis.BeBiS.engine.upgrade.event.UpgradeEvent;
import com.bebis.BeBiS.equipment.domain.Equipment;
import com.bebis.BeBiS.item.domain.Armor;
import com.bebis.BeBiS.item.domain.EquippableItem;
import com.bebis.BeBiS.item.domain.Item;
import com.bebis.BeBiS.item.domain.StatType;
import com.bebis.BeBiS.item.domain.Weapon;
import com.bebis.BeBiS.item.jpa.ArmorEntity;
import com.bebis.BeBiS.item.jpa.ItemEntity;
import com.bebis.BeBiS.item.jpa.WeaponEntity;
import com.bebis.BeBiS.profile.domain.CharacterInfo;
import com.bebis.BeBiS.profile.domain.WowCharacter;
import com.bebis.BeBiS.profile.domain.WowCharacterBuilder;
import com.bebis.BeBiS.profile.domain.WowTalents;
import jakarta.persistence.EntityManager;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static com.bebis.BeBiS.engine.upgrade.event.UpgradeEvent.*;
import static com.bebis.BeBiS.profile.domain.WowCharacter.WowClass;
import static com.bebis.BeBiS.profile.domain.WowTalents.Specialization;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;


@RecordApplicationEvents
public class IsolatedUpgradeOrchestratorIT extends BaseNonTransactionalFullstackTest {

    @Autowired
    private IsolatedUpgradeOrchestrator orchestrator;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ApplicationEvents applicationEvents;

    @AfterEach
    void dbTeardown() {
        // no @Transactional, so manual teardown is needed
        jdbcTemplate.execute("TRUNCATE TABLE items CASCADE"); // postgre-specific syntax
    }

    @ParameterizedTest
    @MethodSource("upgradeFoundArgumentsProvider")
    void shouldSignalUpgradeFoundEventWithTheUpgradeWhenFound(Equipment.Slot slot, Item currentItem, Item bestItem, Item otherItem) {
        // given
        Equipment eq = new Equipment();
        eq.putItem(slot, currentItem, List.of());

        CharacterInfo charInfo = createCharacterInfo(
                WowCharacterBuilder.newWowCharacter().withWowClass(WowClass.ROGUE).withLevel(60).build(), eq,
                Optional.of(createTalents(Map.of("ASSASSINATION", 31, "COMBAT", 20, "SUBTLETY", 0))));

        transactionTemplate.executeWithoutResult((a) -> setUpItemsInDb(List.of(currentItem, bestItem, otherItem)));

        // when
        orchestrator.findUpgrade(slot, charInfo);

        // then
        await().untilAsserted(() -> assertThat(applicationEvents.stream(UpgradeFoundEvent.class))
                .as("Expect an UpgradeFoundEvent with the best item to be recorded for slot " + slot)
                .anyMatch(event -> event.upgrade().equals(bestItem) && event.slot().equals(slot)));
    }

    private static Stream<Arguments> upgradeFoundArgumentsProvider() {
        // CHEST
        Armor currentChest = createArmor(1L, "Current Chest", Item.InventoryType.CHEST, 55,
                Map.of(StatType.ARMOR, 80, StatType.AGILITY, 15, StatType.STRENGTH, 12, StatType.STAMINA, 7), Armor.ArmorType.LEATHER);
        Armor bestChest = createArmor(2L, "Best Chest", Item.InventoryType.CHEST, 60,
                Map.of(StatType.ARMOR, 80, StatType.AGILITY, 22, StatType.STRENGTH, 15, StatType.STAMINA, 10), Armor.ArmorType.LEATHER);
        Armor otherChest = createArmor(3L, "Other Chest", Item.InventoryType.CHEST, 49,
                Map.of(StatType.ARMOR, 80, StatType.AGILITY, 10, StatType.STRENGTH, 10, StatType.STAMINA, 10), Armor.ArmorType.LEATHER);

        // RANGED (Stat stick Guns/Bows for Rogue)
        Weapon currentGun = createRangedWeapon(10L, "Stiff Recoil Gun", 52,
                Map.of(StatType.AGILITY, 7, StatType.STAMINA, 5), 2.6, 45, 85, 25.0, Weapon.WeaponType.GUN);
        Weapon bestBow = createRangedWeapon(11L, "Precise Longbow", 60,
                Map.of(StatType.AGILITY, 14, StatType.STAMINA, 8), 2.8, 60, 110, 30.3, Weapon.WeaponType.BOW);
        Weapon otherGun = createRangedWeapon(12L, "Rusty Musket", 48,
                Map.of(StatType.AGILITY, 4, StatType.STAMINA, 4), 2.4, 35, 65, 20.8, Weapon.WeaponType.GUN);

        return Stream.of(
                Arguments.of(Equipment.Slot.CHEST, currentChest, bestChest, otherChest),
                Arguments.of(Equipment.Slot.RANGED, currentGun, bestBow, otherGun)
        );
    }

    @ParameterizedTest
    @MethodSource("bisAlreadyEquippedArgumentsProvider")
    void shouldSignalBiSAlreadyEquippedEventWhenEquippedItemIsAlreadyBiS(Equipment.Slot slot, Item currentItem, Item lowerItem1, Item lowerItem2) {
        // given
        Equipment eq = new Equipment();
        eq.putItem(slot, currentItem, List.of());

        CharacterInfo charInfo = createCharacterInfo(
                WowCharacterBuilder.newWowCharacter().withWowClass(WowClass.ROGUE).withLevel(60).build(), eq,
                Optional.of(createTalents(Map.of("ASSASSINATION", 31, "COMBAT", 20, "SUBTLETY", 0))));

        transactionTemplate.executeWithoutResult((a) -> setUpItemsInDb(List.of(currentItem, lowerItem1, lowerItem2)));

        // when
        orchestrator.findUpgrade(slot, charInfo);

        // then
        await().untilAsserted(() -> assertThat(applicationEvents.stream(BiSAlreadyEquippedEvent.class))
                .as("Expect a BiSAlreadyEquippedEvent as the currently equipped item is best for slot " + slot)
                .anyMatch(event -> event.slot().equals(slot)));
    }

    private static Stream<Arguments> bisAlreadyEquippedArgumentsProvider() {
        // CHEST
        Armor currentChest = createArmor(1L, "Current Chest", Item.InventoryType.CHEST, 60,
                Map.of(StatType.ARMOR, 120, StatType.AGILITY, 37, StatType.STRENGTH, 21, StatType.STAMINA, 12), Armor.ArmorType.LEATHER);
        Armor chest1 = createArmor(2L, "Some Chest", Item.InventoryType.CHEST, 60,
                Map.of(StatType.ARMOR, 80, StatType.AGILITY, 10, StatType.STRENGTH, 8, StatType.STAMINA, 8), Armor.ArmorType.LEATHER);
        Armor chest2 = createArmor(3L, "Other Chest", Item.InventoryType.CHEST, 49,
                Map.of(StatType.ARMOR, 80, StatType.AGILITY, 7, StatType.STRENGTH, 5, StatType.STAMINA, 5), Armor.ArmorType.LEATHER);

        // RANGED
        Weapon currentBow = createRangedWeapon(10L, "Masterwork Bow", 60,
                Map.of(StatType.AGILITY, 20, StatType.STAMINA, 10), 2.8, 70, 120, 33.9, Weapon.WeaponType.BOW);
        Weapon gun1 = createRangedWeapon(11L, "Average Rifle", 60,
                Map.of(StatType.AGILITY, 8, StatType.STAMINA, 8), 2.6, 50, 90, 26.9, Weapon.WeaponType.GUN);
        Weapon gun2 = createRangedWeapon(12L, "Old Carbine", 50,
                Map.of(StatType.AGILITY, 5, StatType.STAMINA, 5), 2.5, 40, 70, 22.0, Weapon.WeaponType.GUN);

        return Stream.of(
                Arguments.of(Equipment.Slot.CHEST, currentChest, chest1, chest2),
                Arguments.of(Equipment.Slot.RANGED, currentBow, gun1, gun2)
        );
    }

    @Test
    void shouldSignalUpgradeNotAvailableEventWhenItemPoolIsEmpty() {
        // given
        Equipment.Slot slot = Equipment.Slot.CHEST;

        Equipment eq = new Equipment();

        CharacterInfo charInfo = createCharacterInfo(
                WowCharacterBuilder.newWowCharacter().withWowClass(WowClass.ROGUE).build(), eq,
                Optional.of(createTalents(Map.of("ASSASSINATION", 31, "COMBAT", 20, "SUBTLETY", 0))));

        // when
        orchestrator.findUpgrade(slot, charInfo);

        // then
        await().untilAsserted(() -> assertThat(applicationEvents.stream(UpgradeNotAvailableEvent.class))
                .as("Expect an UpgradeNotAvailableEvent as the item pool is empty")
                .anyMatch(event -> event.slot().equals(slot)));
    }

    @ParameterizedTest
    @MethodSource("nonHybridClassTalentsProvider")
    void shouldSignalAppropriateEventsForDifferentTalentsButSameEquipment_NonHybridClass(Map<String, Integer> specs, Class<? extends UpgradeEvent> expectedEvent) {
        // given
        Equipment.Slot slot = Equipment.Slot.CHEST;

        Armor currentChest = createArmor(1L, "Chest", Item.InventoryType.CHEST, 60,
                Map.of(StatType.ARMOR, 80, StatType.AGILITY, 10, StatType.STRENGTH, 20), Armor.ArmorType.LEATHER);

        Equipment eq = new Equipment();
        eq.putItem(slot, currentChest, List.of());

        Armor newChest = createArmor(3L,
                "New Chest", Item.InventoryType.CHEST, 60,
                Map.of(StatType.ARMOR, 80, StatType.AGILITY, 20), Armor.ArmorType.LEATHER);

        transactionTemplate.executeWithoutResult((a) -> setUpItemsInDb(List.of(currentChest, newChest)));

        CharacterInfo charInfo = createCharacterInfo(
                WowCharacterBuilder.newWowCharacter().withLevel(60).withWowClass(WowClass.ROGUE).build(),
                eq, Optional.of(createTalents(specs)));

        // when
        orchestrator.findUpgrade(slot, charInfo);

        // then
        await().untilAsserted(() -> assertThat(applicationEvents.stream(expectedEvent)).isNotEmpty());
    }

    private static Stream<Arguments> nonHybridClassTalentsProvider() {
        return Stream.of(
                Arguments.of(Map.of("ASSASSINATION", 31, "COMBAT", 20, "SUBTLETY", 0), UpgradeFoundEvent.class),
                Arguments.of(Map.of("ASSASSINATION", 20, "COMBAT", 31, "SUBTLETY", 0), BiSAlreadyEquippedEvent.class)
        );
    }

    @ParameterizedTest
    @MethodSource("hybridClassTalentsProvider")
    void shouldSignalAppropriateEventsForDifferentTalentsButSameEquipment_HybridClass(Map<String, Integer> specs, Class<? extends UpgradeEvent> expectedEvent) {
        // given
        Equipment.Slot slot = Equipment.Slot.CHEST;

        Map<StatType, Integer> currentChestStats = Map.of(StatType.ARMOR, 600, StatType.STAMINA, 20, StatType.STRENGTH,
                16, StatType.INTELLECT, 8, StatType.DEFENSE_RATING, 20);

        Armor currentChest = createArmor(1L, "Chest", Item.InventoryType.CHEST, 60, currentChestStats, Armor.ArmorType.PLATE);

        Equipment eq = new Equipment();
        eq.putItem(slot, currentChest, List.of());

        Map<StatType, Integer> newChestStats = Map.of(StatType.ARMOR, 500, StatType.STAMINA, 10, StatType.SPIRIT,
                8, StatType.INTELLECT, 20, StatType.MANA_PER_5_SECONDS, 7);

        Armor newChest = createArmor(3L, "New Chest", Item.InventoryType.CHEST, 60, newChestStats, Armor.ArmorType.PLATE);

        transactionTemplate.executeWithoutResult((a) -> setUpItemsInDb(List.of(currentChest, newChest)));

        CharacterInfo charInfo = createCharacterInfo(
                WowCharacterBuilder.newWowCharacter().withLevel(60).withWowClass(WowClass.PALADIN).build(),
                eq, Optional.of(createTalents(specs)));

        // when
        orchestrator.findUpgrade(slot, charInfo);

        // then
        await().untilAsserted(() -> assertThat(applicationEvents.stream(expectedEvent)).isNotEmpty());
    }

    private static Stream<Arguments> hybridClassTalentsProvider() {
        return Stream.of(
                Arguments.of(Map.of("PROTECTION", 31, "RETRIBUTION", 20, "HOLY", 0), BiSAlreadyEquippedEvent.class),
                Arguments.of(Map.of("PROTECTION", 20, "RETRIBUTION", 0, "HOLY", 31), UpgradeFoundEvent.class)
        );
    }

    @Test
    void shouldSignalUpgradeFoundEventWhenADifferentTypeOfArmorIsBetterForAClassSpec() {
        // given
        Equipment.Slot slot = Equipment.Slot.CHEST;

        // Current: Plate with high armor but low stats
        Armor currentPlate = createArmor(1L, "Plate Chest", Item.InventoryType.CHEST, 60,
                Map.of(StatType.ARMOR, 500, StatType.STRENGTH, 5), Armor.ArmorType.PLATE);

        Equipment eq = new Equipment();
        eq.putItem(slot, currentPlate, List.of());

        Armor upgradeLeather = createArmor(2L, "Leather Chest", Item.InventoryType.CHEST, 60,
                Map.of(StatType.ARMOR, 200, StatType.STRENGTH, 25), Armor.ArmorType.LEATHER);

        transactionTemplate.executeWithoutResult((a) -> setUpItemsInDb(List.of(currentPlate, upgradeLeather)));

        CharacterInfo charInfo = createCharacterInfo(
                WowCharacterBuilder.newWowCharacter().withWowClass(WowClass.WARRIOR).withLevel(60).build(), eq,
                Optional.of(createTalents(Map.of("ARMS", 20, "FURY", 31, "PROTECTION", 0))));

        // when
        orchestrator.findUpgrade(slot, charInfo);

        // then
        await().untilAsserted(() -> assertThat(applicationEvents.stream(UpgradeFoundEvent.class))
                .anyMatch(event -> event.upgrade().equals(upgradeLeather)));
    }

    @Test
    void shouldSignalUpgradeFoundEventWhenArmorIsOnlyDifference() {
        // given
        Equipment.Slot slot = Equipment.Slot.CHEST;

        Armor currentChest = createArmor(1L, "Worn Tunic", Item.InventoryType.CHEST, 1,
                Map.of(StatType.ARMOR, 46), Armor.ArmorType.LEATHER);

        Equipment eq = new Equipment();
        eq.putItem(slot, currentChest, List.of());

        Armor upgradeChest = createArmor(2L, "New Tunic", Item.InventoryType.CHEST, 1,
                Map.of(StatType.ARMOR, 72), Armor.ArmorType.LEATHER);

        transactionTemplate.executeWithoutResult((a) -> setUpItemsInDb(List.of(currentChest, upgradeChest)));

        CharacterInfo charInfo = createCharacterInfo(
                WowCharacterBuilder.newWowCharacter().withLevel(1).withWowClass(WowClass.ROGUE).build(), eq, Optional.empty());

        // when
        orchestrator.findUpgrade(slot, charInfo);

        // then
        await().untilAsserted(() -> assertThat(applicationEvents.stream(UpgradeFoundEvent.class))
                .anyMatch(event -> event.upgrade().equals(upgradeChest)));
    }

    @Test
    void shouldSignalUpgradeFoundEventWhenArmorHasBetterStatsButLessArmor() {
        // given
        Equipment.Slot slot = Equipment.Slot.CHEST;

        Armor currentChest = createArmor(1L, "Worn Breastplate", Item.InventoryType.CHEST, 1,
                Map.of(StatType.ARMOR, 105, StatType.STRENGTH, 4), Armor.ArmorType.MAIL);

        Equipment eq = new Equipment();
        eq.putItem(slot, currentChest, List.of());

        Armor upgradeChest = createArmor(2L, "Shiny Breastplate", Item.InventoryType.CHEST, 1,
                Map.of(StatType.ARMOR, 100, StatType.STRENGTH, 5), Armor.ArmorType.MAIL);

        transactionTemplate.executeWithoutResult((a) -> setUpItemsInDb(List.of(currentChest, upgradeChest)));

        CharacterInfo charInfo = createCharacterInfo(WowCharacterBuilder.newWowCharacter().withLevel(9).withWowClass(WowClass.WARRIOR).build(), eq, Optional.empty());

        // when
        orchestrator.findUpgrade(slot, charInfo);

        // then
        await().untilAsserted(() -> assertThat(applicationEvents.stream(UpgradeFoundEvent.class))
                .anyMatch(event -> event.upgrade().equals(upgradeChest)));
    }

    // domain

    private static Armor createArmor(long id, String name, Item.InventoryType inventoryType, int requiredLevel,
                                     Map<StatType, Integer> stats, Armor.ArmorType armorType) {
        Item.ItemMetadata metadata = new Item.ItemMetadata(
                new Item.ItemKey(id, 0L), name, inventoryType, Item.Quality.UNCOMMON, 21, requiredLevel, false, stats, List.of()
        );
        return new Armor(metadata, armorType);
    }

    private static Weapon createRangedWeapon(long id, String name, int requiredLevel, Map<StatType, Integer> stats, double speed,
                                             int minDamage, int maxDamage, double dps, Weapon.WeaponType type) {
        Item.ItemMetadata metadata = new Item.ItemMetadata(
                new Item.ItemKey(id, 0L), name, Item.InventoryType.RANGED, Item.Quality.UNCOMMON, 21, requiredLevel, false, stats, List.of()
        );
        return new Weapon(metadata, speed, minDamage, maxDamage, dps, type);
    }

    private WowTalents createTalents(Map<String, Integer> specs) {
        List<Specialization> listSpec = specs.entrySet().stream()
                .map((spec) -> new Specialization(spec.getKey(), spec.getValue()))
                .toList();
        return new WowTalents(listSpec);
    }

    private CharacterInfo createCharacterInfo(WowCharacter wowCharacter, Equipment eq, Optional<WowTalents> talents) {
        return new CharacterInfo(wowCharacter, eq, talents);
    }

    // db

    private void setUpItemInDb(Item item) {
        switch (item) {
            case Armor armor -> entityManager.persist(getArmorEntity(armor));
            case Weapon weapon -> entityManager.persist(getWeaponEntity(weapon));
            case EquippableItem equippable -> throw new IllegalArgumentException(
                    "IsolatedUpgradeOrchestratorIT does not support EquippableItem (rings/trinkets). Got: " + equippable);
        }
    }

    private void setUpItemsInDb(List<Item> items) {
        items.forEach(this::setUpItemInDb);
        entityManager.flush();
        entityManager.clear();
    }

    private static void setEntityMetadata(ItemEntity entity, Item.ItemMetadata metadata) {
        entity.setPk(new ItemEntity.CompositeKey(metadata.key().baseId(), 0L));
        entity.setName(metadata.name());
        entity.setQuality(metadata.quality());
        entity.setInventoryType(metadata.inventoryType());
        entity.setItemLevel(metadata.itemLevel());
        entity.setRequiredLevel(metadata.requiredLevel());
        entity.setUniqueEquipped(metadata.uniqueEquipped());
        entity.setStats(metadata.stats());
        entity.setSpecialEffects(metadata.specialEffects());
    }

    private static @NonNull ArmorEntity getArmorEntity(Armor armor) {
        Item.ItemMetadata metadata = armor.getMetadata();
        ArmorEntity entity = new ArmorEntity();
        setEntityMetadata(entity, metadata);

        entity.setArmorType(armor.getArmorType());

        return entity;
    }

    private static @NonNull WeaponEntity getWeaponEntity(Weapon weapon) {
        Item.ItemMetadata metadata = weapon.getMetadata();
        WeaponEntity entity = new WeaponEntity();
        setEntityMetadata(entity, metadata);

        entity.setWeaponType(weapon.getWeaponType());
        entity.setSpeed(weapon.getSpeed());
        entity.setMinDamage(weapon.getMinDamage());
        entity.setMaxDamage(weapon.getMaxDamage());
        entity.setDps(weapon.getDps());

        return entity;
    }
}
