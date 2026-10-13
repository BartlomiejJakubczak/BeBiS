package com.bebis.BeBiS.engine.upgrade;

import com.bebis.BeBiS.base.BaseNonTransactionalFullstackTest;
import com.bebis.BeBiS.engine.upgrade.domain.DualSlot;
import com.bebis.BeBiS.engine.upgrade.event.UpgradeEvent;
import com.bebis.BeBiS.equipment.domain.Equipment;
import com.bebis.BeBiS.item.EquippableItemBuilder;
import com.bebis.BeBiS.item.domain.Item;
import com.bebis.BeBiS.item.jpa.EquippableItemEntity;
import com.bebis.BeBiS.item.jpa.ItemEntity;
import com.bebis.BeBiS.profile.domain.CharacterInfo;
import com.bebis.BeBiS.profile.domain.WowCharacter;
import com.bebis.BeBiS.profile.domain.WowCharacterBuilder;
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

import static com.bebis.BeBiS.equipment.domain.Equipment.Slot.FINGER_1;
import static com.bebis.BeBiS.equipment.domain.Equipment.Slot.FINGER_2;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@RecordApplicationEvents
public class DualSlotUpgradeOrchestratorIT extends BaseNonTransactionalFullstackTest {

    @Autowired
    private DualSlotUpgradeOrchestrator orchestrator;

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
    void shouldSignalUpgradeNotAvailableEventForBothSlots_EmptyItemPool() {
        // given
        WowCharacter wowCharacter = WowCharacterBuilder.newWowCharacter().withLevel(9).build();
        DualSlot dualSlot = new DualSlot(FINGER_1, FINGER_2);

        // when
        orchestrator.findUpgrades(dualSlot, new CharacterInfo(wowCharacter, new Equipment(), Optional.empty()));

        // then
        await().untilAsserted(() -> assertThat(applicationEvents.stream(UpgradeEvent.UpgradeNotAvailableEvent.class))
                .map(UpgradeEvent.UpgradeNotAvailableEvent::slot).containsExactlyInAnyOrder(FINGER_1, FINGER_2));

        verify(itemPoolProvider).getItemPool(eq(dualSlot.slot1()), eq(wowCharacter.level()), eq(wowCharacter.wowClass()));
    }

    @Test
    void shouldSignalUpgradeFoundEventForBothSlots() {
        // given
        WowCharacter wowCharacter = WowCharacterBuilder.newWowCharacter().build();
        DualSlot dualSlot = new DualSlot(FINGER_1, FINGER_2);

        // save an eligible ring to db
        Item theOneRing = EquippableItemBuilder.newRing().withBaseId(1L).withInventoryType("FINGER").build();
        transactionTemplate.executeWithoutResult(a -> setUpItemsInDb(List.of(theOneRing)));

        // when
        orchestrator.findUpgrades(dualSlot, new CharacterInfo(wowCharacter, new Equipment(), Optional.empty()));

        // then
        await().untilAsserted(() -> assertThat(applicationEvents.stream(UpgradeEvent.UpgradeFoundEvent.class))
                .extracting(UpgradeEvent.UpgradeFoundEvent::slot, UpgradeEvent.UpgradeFoundEvent::upgrade)
                .containsExactlyInAnyOrder(
                        tuple(FINGER_1, theOneRing),
                        tuple(FINGER_2, theOneRing)
                )
        );

        verify(itemPoolProvider).getItemPool(eq(dualSlot.slot1()), eq(wowCharacter.level()), eq(wowCharacter.wowClass()));
    }

    @Test
    void shouldSignalBiSAlreadyEquippedEventForBothSlots_CurrentItemsAlreadyBiS() {
        // given
        WowCharacter wowCharacter = WowCharacterBuilder.newWowCharacter().withLevel(30).build();

        Item bisRing = EquippableItemBuilder.newRing().withBaseId(1L)
                .withInventoryType("FINGER")
                .withRequiredLevel(30)
                .withQuality("RARE")
                .withStats(Map.of("AGILITY", 7, "STRENGTH", 5))
                .build();

        Equipment eq = new Equipment();
        eq.putItem(FINGER_1, bisRing, List.of());
        eq.putItem(FINGER_2, bisRing, List.of());

        Item worseRing = EquippableItemBuilder.newRing().withBaseId(2L)
                .withInventoryType("FINGER")
                .withRequiredLevel(25)
                .withQuality("UNCOMMON")
                .withStats(Map.of("AGILITY", 3, "STRENGTH", 3))
                .build();

        transactionTemplate.executeWithoutResult(a -> setUpItemsInDb(List.of(worseRing, bisRing)));

        DualSlot dualSlot = new DualSlot(FINGER_1, FINGER_2);

        // when
        orchestrator.findUpgrades(dualSlot, new CharacterInfo(wowCharacter, eq, Optional.empty()));

        // then
        await().untilAsserted(() -> assertThat(applicationEvents.stream(UpgradeEvent.BiSAlreadyEquippedEvent.class))
                .extracting(UpgradeEvent.BiSAlreadyEquippedEvent::slot, UpgradeEvent.BiSAlreadyEquippedEvent::bis)
                .containsExactlyInAnyOrder(
                        tuple(FINGER_1, bisRing),
                        tuple(FINGER_2, bisRing)
                )
        );

        verify(itemPoolProvider).getItemPool(eq(dualSlot.slot1()), eq(wowCharacter.level()), eq(wowCharacter.wowClass()));
    }

    @Test
    void shouldSignalBiSAlreadyEquippedAndUpgradeFound() {
        // given
        WowCharacter wowCharacter = WowCharacterBuilder.newWowCharacter().build();

        Item bisRing = EquippableItemBuilder.newRing().withBaseId(1L)
                .withInventoryType("FINGER")
                .withQuality("EPIC")
                .withStats(Map.of("AGILITY", 20, "STRENGTH", 5))
                .build();

        Item worseRing = EquippableItemBuilder.newRing().withBaseId(2L)
                .withInventoryType("FINGER")
                .withRequiredLevel(25)
                .withQuality("UNCOMMON")
                .withStats(Map.of("AGILITY", 3, "STRENGTH", 3))
                .build();

        Equipment eq = new Equipment();
        eq.putItem(FINGER_1, bisRing, List.of());
        eq.putItem(FINGER_2, worseRing, List.of());

        transactionTemplate.executeWithoutResult(a -> setUpItemsInDb(List.of(worseRing, bisRing)));

        DualSlot dualSlot = new DualSlot(FINGER_1, FINGER_2);

        // when
        orchestrator.findUpgrades(dualSlot, new CharacterInfo(wowCharacter, eq, Optional.empty()));

        // then

        await().untilAsserted(() -> {
            assertThat(applicationEvents.stream(UpgradeEvent.BiSAlreadyEquippedEvent.class))
                    .extracting(UpgradeEvent.BiSAlreadyEquippedEvent::slot, UpgradeEvent.BiSAlreadyEquippedEvent::bis)
                    .containsExactly(tuple(FINGER_1, bisRing));
            assertThat(applicationEvents.stream(UpgradeEvent.UpgradeFoundEvent.class))
                    .extracting(UpgradeEvent.UpgradeFoundEvent::slot, UpgradeEvent.UpgradeFoundEvent::upgrade)
                    .containsExactly(tuple(FINGER_2, bisRing));
        });

        verify(itemPoolProvider).getItemPool(eq(dualSlot.slot1()), eq(wowCharacter.level()), eq(wowCharacter.wowClass())
        );
    }

    private void setUpEquippableItemInDb(Item item) {
        Item.ItemMetadata metadata = item.getMetadata();

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

        entityManager.persist(entity);
    }

    private void setUpItemsInDb(List<Item> items) {
        items.forEach(this::setUpEquippableItemInDb);
        entityManager.flush();
        entityManager.clear();
    }

}
