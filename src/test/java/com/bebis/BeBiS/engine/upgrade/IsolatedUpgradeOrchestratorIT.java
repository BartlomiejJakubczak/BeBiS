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
import com.bebis.BeBiS.item.jpa.EquippableItemEntity;
import com.bebis.BeBiS.item.jpa.ItemEntity;
import com.bebis.BeBiS.profile.domain.CharacterInfo;
import com.bebis.BeBiS.profile.domain.WowCharacter;
import com.bebis.BeBiS.profile.domain.WowRealm;
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
import static com.bebis.BeBiS.profile.domain.WowCharacter.*;
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

    @Test
    void shouldSignalUpgradeFoundEventWithTheUpgradeWhenFound() {
        // given
        Equipment.Slot slot = Equipment.Slot.CHEST;

        Armor currentChest = createArmor(1L,
                "Current Chest", Item.InventoryType.CHEST, 55, false,
                Map.of(StatType.ARMOR, 80, StatType.AGILITY, 15, StatType.STRENGTH, 12, StatType.STAMINA, 7), Armor.ArmorType.LEATHER);

        Equipment eq = new Equipment();
        eq.putItem(slot, currentChest, List.of());

        transactionTemplate.executeWithoutResult((a) -> setUpItemInDb(currentChest));

        CharacterInfo charInfo = createCharacterInfo(
                createWowCharacter(60, WowClass.ROGUE), eq,
                Optional.of(createTalents(Map.of("ASSASSINATION", 31, "COMBAT", 20, "SUBTLETY", 0))));

        // other items in the pool
        Armor bestChest = createArmor(2L,
                "Best Chest", Item.InventoryType.CHEST, 60, false,
                Map.of(StatType.ARMOR, 80, StatType.AGILITY, 22, StatType.STRENGTH, 15, StatType.STAMINA, 10), Armor.ArmorType.LEATHER);

        Armor otherChest = createArmor(3L,
                "Other Chest", Item.InventoryType.CHEST, 49, false,
                Map.of(StatType.ARMOR, 80, StatType.AGILITY, 10, StatType.STRENGTH, 10, StatType.STAMINA, 10), Armor.ArmorType.LEATHER);

        transactionTemplate.executeWithoutResult((a) -> setUpItemsInDb(List.of(bestChest, otherChest)));

        // when
        orchestrator.findUpgrade(slot, charInfo);

        // then
        await().untilAsserted(() -> assertThat(applicationEvents.stream(UpgradeFoundEvent.class))
                .as("Expect an UpgradeFoundEvent with the best chest piece to be recorded")
                .anyMatch(event -> event.upgrade().equals(bestChest) && event.slot().equals(slot)));
    }

    @Test
    void shouldSignalBiSAlreadyEquippedEventWhenEquippedItemIsAlreadyBiS() {
        // given
        Equipment.Slot slot = Equipment.Slot.CHEST;

        Armor currentChest = createArmor(1L,
                "Current Chest", Item.InventoryType.CHEST, 60, false,
                Map.of(StatType.ARMOR, 120, StatType.AGILITY, 37, StatType.STRENGTH, 21, StatType.STAMINA, 12), Armor.ArmorType.LEATHER);

        Equipment eq = new Equipment();
        eq.putItem(slot, currentChest, List.of());

        CharacterInfo charInfo = createCharacterInfo(
                createWowCharacter(60, WowClass.ROGUE), eq,
                Optional.of(createTalents(Map.of("ASSASSINATION", 31, "COMBAT", 20, "SUBTLETY", 0))));

        // other items in the pool
        Armor chest1 = createArmor(2L,
                "Some Chest", Item.InventoryType.CHEST, 60, false,
                Map.of(StatType.ARMOR, 80, StatType.AGILITY, 10, StatType.STRENGTH, 8, StatType.STAMINA, 8), Armor.ArmorType.LEATHER);

        Armor chest2 = createArmor(3L,
                "Other Chest", Item.InventoryType.CHEST, 49, false,
                Map.of(StatType.ARMOR, 80, StatType.AGILITY, 7, StatType.STRENGTH, 5, StatType.STAMINA, 5), Armor.ArmorType.LEATHER);

        transactionTemplate.executeWithoutResult((a) -> setUpItemsInDb(List.of(currentChest, chest1, chest2)));

        // when
        orchestrator.findUpgrade(slot, charInfo);

        // then
        await().untilAsserted(() -> assertThat(applicationEvents.stream(BiSAlreadyEquippedEvent.class))
                .as("Expect an BiSAlreadyEquippedEvent as the current chest is the best")
                .anyMatch(event -> event.slot().equals(slot)));
    }

    @Test
    void shouldSignalUpgradeNotAvailableEventWhenItemPoolIsEmpty() {
        // given
        Equipment.Slot slot = Equipment.Slot.CHEST;

        Equipment eq = new Equipment();

        CharacterInfo charInfo = createCharacterInfo(
                createWowCharacter(60, WowClass.ROGUE), eq,
                Optional.of(createTalents(Map.of("ASSASSINATION", 31, "COMBAT", 20, "SUBTLETY", 0))));

        // when
        orchestrator.findUpgrade(slot, charInfo);

        // then
        await().untilAsserted(() -> assertThat(applicationEvents.stream(UpgradeNotAvailableEvent.class))
                .as("Expect an UpgradeNotAvailableEvent as the item pool is empty")
                .anyMatch(event -> event.slot().equals(slot)));
    }

    @Test
    void shouldSignalUpgradeNotAvailableEventWhenItemsInDbAreAboveCharacterLevel() {
        // given
        int chestPieceReqLevel = 30;
        Armor chest = createArmor(3L, "Chest", Item.InventoryType.CHEST, chestPieceReqLevel, false,
                Map.of(StatType.ARMOR, 120, StatType.AGILITY, 37, StatType.STRENGTH, 21, StatType.STAMINA, 12), Armor.ArmorType.LEATHER);

        // setup db items
        transactionTemplate.executeWithoutResult((a) -> setUpItemsInDb(List.of(chest)));

        Equipment eq = new Equipment();

        CharacterInfo charInfo = createCharacterInfo(
                createWowCharacter(chestPieceReqLevel - 1, WowClass.ROGUE), eq,
                Optional.of(createTalents(Map.of("ASSASSINATION", 31, "COMBAT", 20, "SUBTLETY", 0))));

        // when
        orchestrator.findUpgrade(Equipment.Slot.CHEST, charInfo);

        // then
        await().untilAsserted(() -> assertThat(applicationEvents.stream(UpgradeNotAvailableEvent.class))
                .as("Expect an UpgradeNotAvailableEvent as the only chest piece available in DB is above the req level")
                .anyMatch(event -> event.slot().equals(Equipment.Slot.CHEST)));
    }

    @ParameterizedTest
    @MethodSource("classAndLevelToItemTypeProvider")
    void shouldSignalAppropriateEventsWhenItemsInDbAreTransitionalTypes(WowClass wowClass, int charLevel, Armor.ArmorType transitionalType, Class<? extends UpgradeEvent> expectedEvent) {
        // given
        Equipment.Slot slot = Equipment.Slot.CHEST;
        int armorTransitionLevel = 40;

        Armor chest = createArmor(1L, "Chest", Item.InventoryType.CHEST, armorTransitionLevel, false,
                Map.of(StatType.ARMOR, 80, StatType.AGILITY, 5, StatType.STRENGTH, 5, StatType.STAMINA, 5), transitionalType);

        transactionTemplate.executeWithoutResult((a) -> setUpItemInDb(chest));

        CharacterInfo charInfo = createCharacterInfo(
                createWowCharacter(charLevel, wowClass), new Equipment(),
                Optional.of(createTalents(Map.of("TREE1", 31, "TREE2", 20, "TREE3", 0))));

        // when
        orchestrator.findUpgrade(slot, charInfo);

        // then
        await().untilAsserted(() -> assertThat(applicationEvents.stream(expectedEvent)).isNotEmpty());
    }

    private static Stream<Arguments> classAndLevelToItemTypeProvider() {
        return Stream.of(
                Arguments.of(WowClass.WARRIOR, 39, Armor.ArmorType.PLATE, UpgradeNotAvailableEvent.class),
                Arguments.of(WowClass.WARRIOR, 40, Armor.ArmorType.PLATE, UpgradeFoundEvent.class),
                Arguments.of(WowClass.PALADIN, 39, Armor.ArmorType.PLATE, UpgradeNotAvailableEvent.class),
                Arguments.of(WowClass.PALADIN, 40, Armor.ArmorType.PLATE, UpgradeFoundEvent.class),
                Arguments.of(WowClass.HUNTER, 39, Armor.ArmorType.MAIL, UpgradeNotAvailableEvent.class),
                Arguments.of(WowClass.HUNTER, 40, Armor.ArmorType.MAIL, UpgradeFoundEvent.class),
                Arguments.of(WowClass.SHAMAN, 39, Armor.ArmorType.MAIL, UpgradeNotAvailableEvent.class),
                Arguments.of(WowClass.SHAMAN, 40, Armor.ArmorType.MAIL, UpgradeFoundEvent.class),
                // control group - no accidental armor type transitions for not eligible class
                Arguments.of(WowClass.ROGUE, 40, Armor.ArmorType.MAIL, UpgradeNotAvailableEvent.class),
                Arguments.of(WowClass.ROGUE, 40, Armor.ArmorType.PLATE, UpgradeNotAvailableEvent.class)
        );
    }

    @ParameterizedTest
    @MethodSource("nonHybridClassTalentsProvider")
    void shouldSignalAppropriateEventsForDifferentTalentsButSameEquipment_NonHybridClass(Map<String, Integer> specs, Class<? extends UpgradeEvent> expectedEvent) {
        // given
        Equipment.Slot slot = Equipment.Slot.CHEST;
        int charLevel = 60;
        WowClass wowClass = WowClass.ROGUE;

        Armor currentChest = createArmor(1L, "Chest", Item.InventoryType.CHEST, 60, false,
                Map.of(StatType.ARMOR, 80, StatType.AGILITY, 10, StatType.STRENGTH, 20), Armor.ArmorType.LEATHER);

        Equipment eq = new Equipment();
        eq.putItem(slot, currentChest, List.of());

        Armor newChest = createArmor(3L,
                "New Chest", Item.InventoryType.CHEST, 60, false,
                Map.of(StatType.ARMOR, 80, StatType.AGILITY, 20), Armor.ArmorType.LEATHER);

        transactionTemplate.executeWithoutResult((a) -> setUpItemsInDb(List.of(currentChest, newChest)));

        CharacterInfo charInfo = createCharacterInfo(createWowCharacter(charLevel, wowClass), eq, Optional.of(createTalents(specs)));

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
        int charLevel = 60;
        WowClass wowClass = WowClass.PALADIN;

        Armor currentChest = createArmor(1L, "Chest", Item.InventoryType.CHEST, 60, false,
                Map.of(StatType.ARMOR, 600, StatType.STAMINA, 20, StatType.STRENGTH, 16, StatType.INTELLECT, 8, StatType.DEFENSE_RATING, 20), Armor.ArmorType.PLATE);

        Equipment eq = new Equipment();
        eq.putItem(slot, currentChest, List.of());

        Armor newChest = createArmor(3L,
                "New Chest", Item.InventoryType.CHEST, 60, false,
                Map.of(StatType.ARMOR, 500, StatType.STAMINA, 10, StatType.SPIRIT, 8, StatType.INTELLECT, 20, StatType.MANA_PER_5_SECONDS, 7), Armor.ArmorType.PLATE);

        transactionTemplate.executeWithoutResult((a) -> setUpItemsInDb(List.of(currentChest, newChest)));

        CharacterInfo charInfo = createCharacterInfo(createWowCharacter(charLevel, wowClass), eq, Optional.of(createTalents(specs)));

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
        Armor currentPlate = createArmor(1L, "Plate Chest", Item.InventoryType.CHEST, 60, false,
                Map.of(StatType.ARMOR, 500, StatType.STRENGTH, 5), Armor.ArmorType.PLATE);

        Equipment eq = new Equipment();
        eq.putItem(slot, currentPlate, List.of());

        Armor upgradeLeather = createArmor(2L, "Leather Chest", Item.InventoryType.CHEST, 60, false,
                Map.of(StatType.ARMOR, 200, StatType.STRENGTH, 25), Armor.ArmorType.LEATHER);

        transactionTemplate.executeWithoutResult((a) -> setUpItemsInDb(List.of(currentPlate, upgradeLeather)));

        CharacterInfo charInfo = createCharacterInfo(createWowCharacter(60, WowClass.WARRIOR), eq,
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

        Armor currentChest = createArmor(1L, "Worn Tunic", Item.InventoryType.CHEST, 1, false,
                Map.of(StatType.ARMOR, 46), Armor.ArmorType.LEATHER);

        Equipment eq = new Equipment();
        eq.putItem(slot, currentChest, List.of());

        Armor upgradeChest = createArmor(2L, "New Tunic", Item.InventoryType.CHEST, 1, false,
                Map.of(StatType.ARMOR, 72), Armor.ArmorType.LEATHER);

        transactionTemplate.executeWithoutResult((a) -> setUpItemsInDb(List.of(currentChest, upgradeChest)));

        CharacterInfo charInfo = createCharacterInfo(createWowCharacter(1, WowClass.ROGUE), eq, Optional.empty());

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

        Armor currentChest = createArmor(1L, "Worn Breastplate", Item.InventoryType.CHEST, 1, false,
                Map.of(StatType.ARMOR, 105, StatType.STRENGTH, 4), Armor.ArmorType.MAIL);

        Equipment eq = new Equipment();
        eq.putItem(slot, currentChest, List.of());

        Armor upgradeChest = createArmor(2L, "Shiny Breastplate", Item.InventoryType.CHEST, 1, false,
                Map.of(StatType.ARMOR, 100, StatType.STRENGTH, 5), Armor.ArmorType.MAIL);

        transactionTemplate.executeWithoutResult((a) -> setUpItemsInDb(List.of(currentChest, upgradeChest)));

        CharacterInfo charInfo = createCharacterInfo(createWowCharacter(9, WowClass.WARRIOR), eq, Optional.empty());

        // when
        orchestrator.findUpgrade(slot, charInfo);

        // then
        await().untilAsserted(() -> assertThat(applicationEvents.stream(UpgradeFoundEvent.class))
                .anyMatch(event -> event.upgrade().equals(upgradeChest)));
    }

    // domain

    private Armor createArmor(long id, String name, Item.InventoryType inventoryType, int requiredLevel, boolean uniqueEquipped,
                              Map<StatType, Integer> stats, Armor.ArmorType armorType) {
        Item.ItemMetadata metadata = new Item.ItemMetadata(
                new Item.ItemKey(id, 0L), name, inventoryType, Item.Quality.UNCOMMON, 21, requiredLevel, uniqueEquipped, stats, List.of()
        );
        return new Armor(metadata, armorType);
    }

    private WowCharacter createWowCharacter(int level, WowClass wowClass) {
        return new WowCharacter(
                new Id(1L, "soulseeker"),
                "Thelamar",
                level,
                Race.NIGHT_ELF,
                wowClass,
                new WowRealm("Soulseeker", "soulseeker")
        );
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
            case EquippableItem equippableItem -> {
                EquippableItemEntity entity = getEquippableItemEntity(equippableItem);
                entityManager.persist(entity);
            }
            case Armor armor -> {
                ArmorEntity entity = getArmorEntity(armor);
                entityManager.persist(entity);
            }
            case Weapon weapon -> {
            } // this orchestrator doesn't deal with weapons
        }
    }

    private void setUpItemsInDb(List<Item> items) {
        items.forEach(this::setUpItemInDb);
        entityManager.flush();
        entityManager.clear();
    }

    private static @NonNull EquippableItemEntity getEquippableItemEntity(EquippableItem equippableItem) {
        Item.ItemMetadata metadata = equippableItem.getMetadata();
        EquippableItemEntity entity = new EquippableItemEntity();
        entity.setPk(new ItemEntity.CompositeKey(metadata.key().baseId(), 0L));
        entity.setName(metadata.name());
        entity.setQuality(metadata.quality());
        entity.setInventoryType(metadata.inventoryType());
        entity.setItemLevel(metadata.itemLevel());
        entity.setRequiredLevel(metadata.requiredLevel());
        entity.setUniqueEquipped(metadata.uniqueEquipped());
        entity.setStats(metadata.stats());
        entity.setSpecialEffects(metadata.specialEffects());
        return entity;
    }

    private static @NonNull ArmorEntity getArmorEntity(Armor armor) {
        Item.ItemMetadata metadata = armor.getMetadata();
        ArmorEntity entity = new ArmorEntity();
        entity.setPk(new ItemEntity.CompositeKey(metadata.key().baseId(), 0L));
        entity.setName(metadata.name());
        entity.setQuality(metadata.quality());
        entity.setInventoryType(metadata.inventoryType());
        entity.setItemLevel(metadata.itemLevel());
        entity.setRequiredLevel(metadata.requiredLevel());
        entity.setUniqueEquipped(metadata.uniqueEquipped());
        entity.setStats(metadata.stats());
        entity.setSpecialEffects(metadata.specialEffects());
        entity.setArmorType(armor.getArmorType());
        return entity;
    }
}
