package com.bebis.BeBiS.engine.upgrade;

import com.bebis.BeBiS.base.BaseNonTransactionalFullstackTest;
import com.bebis.BeBiS.engine.upgrade.domain.DualSlot;
import com.bebis.BeBiS.engine.upgrade.event.UpgradeEvent;
import com.bebis.BeBiS.equipment.domain.Equipment;
import com.bebis.BeBiS.item.WeaponBuilder;
import com.bebis.BeBiS.item.domain.Armor;
import com.bebis.BeBiS.item.domain.Item;
import com.bebis.BeBiS.item.domain.Weapon;
import com.bebis.BeBiS.item.jpa.ArmorEntity;
import com.bebis.BeBiS.item.jpa.ItemEntity;
import com.bebis.BeBiS.item.jpa.WeaponEntity;
import com.bebis.BeBiS.profile.domain.CharacterInfo;
import com.bebis.BeBiS.profile.domain.WowCharacter;
import com.bebis.BeBiS.profile.domain.WowCharacterBuilder;
import com.bebis.BeBiS.profile.domain.WowTalents;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.bebis.BeBiS.equipment.domain.Equipment.Slot.MAIN_HAND;
import static com.bebis.BeBiS.equipment.domain.Equipment.Slot.OFF_HAND;
import static com.bebis.BeBiS.profile.domain.WowCharacter.WowClass.WARRIOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@RecordApplicationEvents
public class HandSlotUpgradeOrchestratorIT extends BaseNonTransactionalFullstackTest {

    @Autowired
    private HandSlotUpgradeOrchestrator orchestrator;

    @MockitoSpyBean
    private ItemPoolProvider itemPoolProvider;

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
    void shouldSignalUpgradeFoundAndSlotClearedEvents_TwoHandBetterThanTheCurrentCombination() {
        // given
        WowCharacter wowCharacter = WowCharacterBuilder.newWowCharacter().withWowClass(WARRIOR).withLevel(55).build();

        Weapon currentMainHand = WeaponBuilder.newOneHandSword().withBaseId(1L).withName("Current Main Sword")
                .withRequiredLevel(52)
                .withWeaponType("SWORD")
                .withMinDamage(80)
                .withMaxDamage(100)
                .withSpeed(2.0)
                .withDps(45.0)
                .build();

        Weapon currentOffHand = WeaponBuilder.newOneHandSword().withBaseId(2L).withName("Current Off Axe")
                .withRequiredLevel(48)
                .withWeaponType("AXE")
                .withMinDamage(70)
                .withMaxDamage(90)
                .withSpeed(2.0)
                .withDps(40.0)
                .build();

        Equipment eq = new Equipment();
        eq.putItem(MAIN_HAND, currentMainHand, List.of());
        eq.putItem(OFF_HAND, currentOffHand, List.of());

        Weapon potentialOneHandUpgrade = WeaponBuilder.newOneHandSword().withBaseId(3L).withName("Cool Sword")
                .withRequiredLevel(55)
                .withWeaponType("SWORD")
                .withMinDamage(100)
                .withMaxDamage(130)
                .withSpeed(2.0)
                .withDps(57.5)
                .build();

        Weapon powerfulEligibleTwoHand = WeaponBuilder.newOneHandSword().withBaseId(4L).withName("Uber Sword")
                .withRequiredLevel(55)
                .withWeaponType("SWORD")
                .withInventoryType("TWO_HAND")
                .withMinDamage(190)
                .withMaxDamage(260)
                .withSpeed(3.0)
                .withDps(75.0)
                .build();

        transactionTemplate.executeWithoutResult(a -> setUpWeaponsInDb(List.of(currentMainHand, currentOffHand,
                potentialOneHandUpgrade, powerfulEligibleTwoHand, createPowerfulOneHand(5L), createPowerfulTwoHand(6L))));

        // when
        orchestrator.findUpgrades(new DualSlot(MAIN_HAND, OFF_HAND), new CharacterInfo(wowCharacter, eq,
                Optional.of(createTalents(Map.of("ARMS", 31, "FURY", 20, "PROTECTION", 0)))));

        // then
        await().untilAsserted(() -> {
            assertThat(applicationEvents.stream(UpgradeEvent.UpgradeFoundEvent.class))
                    .extracting(UpgradeEvent.UpgradeFoundEvent::slot, UpgradeEvent.UpgradeFoundEvent::upgrade)
                    .containsExactly(tuple(MAIN_HAND, powerfulEligibleTwoHand));
            assertThat(applicationEvents.stream(UpgradeEvent.SlotClearedEvent.class))
                    .extracting(UpgradeEvent.SlotClearedEvent::slot, UpgradeEvent.SlotClearedEvent::triggeringItem)
                    .containsExactly(tuple(OFF_HAND, powerfulEligibleTwoHand));
        });

        verify(itemPoolProvider).getItemPool(eq(MAIN_HAND), eq(wowCharacter.level()), eq(wowCharacter.wowClass()));
        verify(itemPoolProvider).getItemPool(eq(OFF_HAND), eq(wowCharacter.level()), eq(wowCharacter.wowClass()));
    }

    private Weapon createPowerfulOneHand(long baseId) {
        return WeaponBuilder.newOneHandSword().withBaseId(baseId).withName("Powerful One Hand")
                .withRequiredLevel(60)
                .withWeaponType("SWORD")
                .withMinDamage(140)
                .withMaxDamage(180)
                .withSpeed(2.0)
                .withDps(80.0)
                .build();
    }

    private WowTalents createTalents(Map<String, Integer> specs) {
        List<WowTalents.Specialization> listSpec = specs.entrySet().stream()
                .map((spec) -> new WowTalents.Specialization(spec.getKey(), spec.getValue()))
                .toList();
        return new WowTalents(listSpec);
    }

    private Weapon createPowerfulTwoHand(long baseId) {
        return WeaponBuilder.newOneHandSword().withBaseId(baseId).withName("Powerful Two Hand")
                .withRequiredLevel(60)
                .withWeaponType("SWORD")
                .withInventoryType("TWO_HAND")
                .withMinDamage(320)
                .withMaxDamage(380)
                .withSpeed(3.5)
                .withDps(100.0)
                .build();
    }

    private void setUpItemMetadata(Item.ItemMetadata metadata, ItemEntity entity) {
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

    private void setUpWeaponInDb(Weapon weapon) {
        WeaponEntity entity = new WeaponEntity();
        setUpItemMetadata(weapon.getMetadata(), entity);

        entity.setWeaponType(weapon.getWeaponType());
        entity.setMinDamage(weapon.getMinDamage());
        entity.setMaxDamage(weapon.getMaxDamage());
        entity.setDps(weapon.getDps());
        entity.setSpeed(weapon.getSpeed());

        entityManager.persist(entity);
    }

    private void setUpWeaponsInDb(List<Weapon> weapons) {
        weapons.forEach(this::setUpWeaponInDb);
        entityManager.flush();
        entityManager.clear();
    }

    private void setUpArmorInDb(Armor armor) {
        ArmorEntity entity = new ArmorEntity();
        setUpItemMetadata(armor.getMetadata(), entity);

        entity.setArmorType(armor.getArmorType());

        entityManager.persist(entity);
    }

    private void setUpArmorsInDb(List<Armor> armors) {
        armors.forEach(this::setUpArmorInDb);
        entityManager.flush();
        entityManager.clear();
    }
}
