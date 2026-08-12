package com.bebis.BeBiS.equipment;

import com.bebis.BeBiS.base.BaseFullStackTest;
import com.bebis.BeBiS.equipment.domain.Equipment;
import com.bebis.BeBiS.equipment.jpa.EquipmentEntity;
import com.bebis.BeBiS.integration.blizzard.dto.EquipmentResponse;
import com.bebis.BeBiS.integration.blizzard.dto.ItemResponse;
import com.bebis.BeBiS.item.ItemResponseBuilder;
import com.bebis.BeBiS.item.domain.Item;
import com.bebis.BeBiS.item.jpa.EquippableItemEntity;
import com.bebis.BeBiS.item.jpa.ItemEntity;
import com.bebis.BeBiS.profile.domain.WowCharacter;
import com.bebis.BeBiS.profile.jpa.WowCharacterEntity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class EquipmentServiceIT extends BaseFullStackTest {

    @Autowired
    private EquipmentService service;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldMapEquipmentToCorrectSlots() {
        // given
        WowCharacterEntity charEntity = setUpCharacterInDb(new HashMap<>());
        WowCharacterEntity.CompositeKey charKey = charEntity.getPk();
        String realmSlug = charEntity.getPk().getRealmSlug();
        String charName = charEntity.getName();

        ItemResponse response = ItemResponseBuilder.newWeaponInstance().build();
        EquipmentResponse.ItemDTO dto = EquipmentResponseBuilder.newInstance(response)
                .withSlot("MAIN_HAND")
                .build();
        EquipmentResponse eqResponse = new EquipmentResponse(List.of(dto));

        when(blizzardUserClient.getCharacterEquipment(eq(realmSlug), eq(charName))).thenReturn(eqResponse);
        when(blizzardServiceClient.getBaseItem(response.id())).thenReturn(response);

        // when
        callService(charEntity);

        // then
        WowCharacterEntity persistedEntity = entityManager.find(WowCharacterEntity.class, charKey);
        Map<Equipment.Slot, EquipmentEntity.EquippedItem> items = persistedEntity.getEquipment().getItems();

        assertThat(items).hasSize(1);
        assertThat(items).containsKey(Equipment.Slot.MAIN_HAND);

        verify(blizzardServiceClient).getBaseItem(response.id());
        verify(blizzardUserClient).getCharacterEquipment(eq(realmSlug), eq(charName));
    }

    @Test
    void shouldSkipBadSlotButPersistTheRestOfEquipment() {
        // given
        WowCharacterEntity charEntity = setUpCharacterInDb(new HashMap<>());
        WowCharacterEntity.CompositeKey charKey = charEntity.getPk();
        String realmSlug = charEntity.getPk().getRealmSlug();
        String charName = charEntity.getName();

        ItemResponse response = ItemResponseBuilder.newWeaponInstance().build();
        EquipmentResponse.ItemDTO wrongDto = EquipmentResponseBuilder.newInstance(response)
                .withSlot("LEFT_HAND")
                .build();

        ItemResponse ring = ItemResponseBuilder.newEquippableInstance().build();
        EquipmentResponse.ItemDTO ringDto = EquipmentResponseBuilder.newInstance(ring)
                .withSlot("FINGER_1")
                .build();

        EquipmentResponse eqResponse = new EquipmentResponse(List.of(wrongDto, ringDto));

        when(blizzardUserClient.getCharacterEquipment(eq(realmSlug), eq(charName))).thenReturn(eqResponse);
        when(blizzardServiceClient.getBaseItem(ring.id())).thenReturn(ring);
        when(blizzardServiceClient.getBaseItem(response.id())).thenReturn(response);

        // when
        callService(charEntity);

        // then
        Map<Equipment.Slot, EquipmentEntity.EquippedItem> items = entityManager
                .find(WowCharacterEntity.class, charKey)
                .getEquipment()
                .getItems();

        assertThat(items).hasSize(1);
        assertThat(items).containsKey(Equipment.Slot.FINGER_1);
        assertThat(items).doesNotContainKey(Equipment.Slot.MAIN_HAND);

        verify(blizzardServiceClient, times(2)).getBaseItem(anyLong());
        verify(blizzardServiceClient).getBaseItem(ring.id());
        verify(blizzardUserClient).getCharacterEquipment(eq(realmSlug), eq(charName));
    }

    @Test
    void shouldPersistPlayerEnchants() {
        // given
        WowCharacterEntity charEntity = setUpCharacterInDb(new HashMap<>());
        WowCharacterEntity.CompositeKey charKey = charEntity.getPk();
        String realmSlug = charEntity.getPk().getRealmSlug();
        String charName = charEntity.getName();

        ItemResponse response = ItemResponseBuilder.newWeaponInstance().build();

        String enchantName = "Crusader";

        EquipmentResponse.ItemDTO dto = EquipmentResponseBuilder.newInstance(response)
                .withSlot("main_hand")
                .withEnchantments(Map.of(1L, enchantName))
                .build();

        EquipmentResponse eqResponse = new EquipmentResponse(List.of(dto));

        when(blizzardUserClient.getCharacterEquipment(eq(realmSlug), eq(charName))).thenReturn(eqResponse);
        when(blizzardServiceClient.getBaseItem(response.id())).thenReturn(response);

        // when
        callService(charEntity);

        // then
        Map<Equipment.Slot, EquipmentEntity.EquippedItem> items = entityManager
                .find(WowCharacterEntity.class, charKey)
                .getEquipment()
                .getItems();

        assertThat(items).hasSize(1);
        assertThat(items).containsKey(Equipment.Slot.MAIN_HAND);
        assertThat(items.get(Equipment.Slot.MAIN_HAND).getPlayerEnchants()).containsExactly(enchantName);

        verify(blizzardServiceClient).getBaseItem(response.id());
        verify(blizzardUserClient).getCharacterEquipment(eq(realmSlug), eq(charName));
    }

    @Test
    void shouldClearOldEquippedItemsFromDbWhenReplaced() {
        // given
        Map<Equipment.Slot, EquipmentEntity.EquippedItem> previousSnapshot = new HashMap<>();
        previousSnapshot.put(Equipment.Slot.FINGER_1, setUpEquippedItem(123L, "Ring of the Past"));

        WowCharacterEntity charEntity = setUpCharacterInDb(previousSnapshot);
        WowCharacterEntity.CompositeKey charKey = charEntity.getPk();
        String realmSlug = charEntity.getPk().getRealmSlug();
        String charName = charEntity.getName();

        String newItemName = "Greatseal";
        ItemResponse ring = ItemResponseBuilder.newEquippableInstance()
                .withName(newItemName)
                .build();
        EquipmentResponse.ItemDTO ringDto = EquipmentResponseBuilder.newInstance(ring)
                .withSlot("finger_1")
                .build();

        EquipmentResponse eqResponse = new EquipmentResponse(List.of(ringDto));

        when(blizzardUserClient.getCharacterEquipment(eq(realmSlug), eq(charName))).thenReturn(eqResponse);
        when(blizzardServiceClient.getBaseItem(ring.id())).thenReturn(ring);

        // when
        callService(charEntity);

        // then
        Map<Equipment.Slot, EquipmentEntity.EquippedItem> items = entityManager
                .find(WowCharacterEntity.class, charKey)
                .getEquipment()
                .getItems();

        assertThat(items).hasSize(1);
        assertThat(items).containsKey(Equipment.Slot.FINGER_1);
        assertThat(items.get(Equipment.Slot.FINGER_1).getItem().getName()).isEqualTo(newItemName);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM equipped_items WHERE base_id = ? AND suffix_id = ?",
                Integer.class, 123L, 0L);

        assertThat(count).isEqualTo(0);

        verify(blizzardServiceClient).getBaseItem(ring.id());
        verify(blizzardUserClient).getCharacterEquipment(eq(realmSlug), eq(charName));
    }

    @Test
    void shouldClearOldEquippedItemsFromDbWhenEmptyResponse() {
        // given
        Map<Equipment.Slot, EquipmentEntity.EquippedItem> previousSnapshot = new HashMap<>();
        previousSnapshot.put(Equipment.Slot.FINGER_1, setUpEquippedItem(123L, "Ring of the Past"));
        previousSnapshot.put(Equipment.Slot.FINGER_2, setUpEquippedItem(456L, "Ring of the Future"));

        WowCharacterEntity charEntity = setUpCharacterInDb(previousSnapshot);
        WowCharacterEntity.CompositeKey charKey = charEntity.getPk();
        String realmSlug = charEntity.getPk().getRealmSlug();
        String charName = charEntity.getName();

        when(blizzardUserClient.getCharacterEquipment(eq(realmSlug), eq(charName)))
                .thenReturn(new EquipmentResponse(List.of()));

        // when
        callService(charEntity);

        // then
        Map<Equipment.Slot, EquipmentEntity.EquippedItem> items = entityManager
                .find(WowCharacterEntity.class, charKey)
                .getEquipment()
                .getItems();
        assertThat(items).isEmpty();

        List<Long> savedEquippedItems = jdbcTemplate.queryForList("SELECT id FROM equipped_items WHERE base_id IN (?, ?)",
                Long.class, 123L, 456L);
        assertThat(savedEquippedItems).isEmpty();

        verifyNoInteractions(blizzardServiceClient);
        verify(blizzardUserClient).getCharacterEquipment(eq(realmSlug), eq(charName));
    }

    @Test
    void shouldPullBaseDataOfEquippedItemFromDbWhenItemExists() {
        // given
        WowCharacterEntity charEntity = setUpCharacterInDb(new HashMap<>());
        ItemEntity item = setUpItemInDb(1L, "Greatseal");

        ItemResponse itemResponse = ItemResponseBuilder.newEquippableInstance()
                .withId(item.getPk().getBaseId())
                .withName(item.getName())
                .build();

        EquipmentResponse.ItemDTO itemDTO = EquipmentResponseBuilder.newInstance(itemResponse)
                .withSlot("finger_1")
                .build();

        when(blizzardUserClient.getCharacterEquipment(eq(charEntity.getPk().getRealmSlug()), eq(charEntity.getName())))
                .thenReturn(new EquipmentResponse(List.of(itemDTO)));

        // when
        callService(charEntity);

        // then
        verifyNoInteractions(blizzardServiceClient); // no need to pull data from blizzard for item that exists in db
    }

    @Test
    void shouldPersistValidItemsWhenOneItemFailsToResolve() {
        // given
        WowCharacterEntity charEntity = setUpCharacterInDb(new HashMap<>());
        WowCharacterEntity.CompositeKey charKey = charEntity.getPk();
        String realmSlug = charEntity.getPk().getRealmSlug();
        String charName = charEntity.getName();

        ItemResponse ringResponse = ItemResponseBuilder.newEquippableInstance()
                .withId(1L)
                .build();
        EquipmentResponse.ItemDTO validDTO = EquipmentResponseBuilder.newInstance(ringResponse)
                .withSlot("finger_1")
                .build();

        ItemResponse trinketResponse = ItemResponseBuilder.newEquippableInstance()
                .withId(2L)
                .build();
        EquipmentResponse.ItemDTO corruptDTO = EquipmentResponseBuilder.newInstance(trinketResponse)
                .withSlot("trinket_1")
                .withFullName(null)
                .build();

        when(blizzardServiceClient.getBaseItem(ringResponse.id())).thenReturn(ringResponse);
        when(blizzardServiceClient.getBaseItem(trinketResponse.id())).thenReturn(trinketResponse);

        when(blizzardUserClient.getCharacterEquipment(realmSlug, charName)).thenReturn(new EquipmentResponse(List.of(validDTO, corruptDTO)));

        // when
        callService(charEntity);

        // then
        Map<Equipment.Slot, EquipmentEntity.EquippedItem> items = entityManager
                .find(WowCharacterEntity.class, charKey)
                .getEquipment()
                .getItems();

        verify(blizzardUserClient).getCharacterEquipment(eq(realmSlug), eq(charName));
        verify(blizzardServiceClient).getBaseItem(ringResponse.id());
        verify(blizzardServiceClient).getBaseItem(trinketResponse.id()); // the bad item was tried

        assertThat(items.size()).isEqualTo(1);
        assertThat(items.get(Equipment.Slot.FINGER_1)).isNotNull();

        EquipmentEntity.EquippedItem equippedItem = items.get(Equipment.Slot.FINGER_1);

        assertThat(equippedItem.getItem().getPk().getBaseId()).isEqualTo(ringResponse.id());
    }

    private EquipmentEntity.EquippedItem setUpEquippedItem(long id, String name) {
        ItemEntity item = setUpItemInDb(id, name);
        EquipmentEntity.EquippedItem previousEqItem = new EquipmentEntity.EquippedItem();
        previousEqItem.setItem(item);
        return previousEqItem;
    }

    private ItemEntity setUpItemInDb(long id, String name) {
        ItemEntity item = new EquippableItemEntity();
        item.setPk(new ItemEntity.CompositeKey(id, 0L));
        item.setName(name);
        item.setQuality(Item.Quality.UNCOMMON);
        item.setInventoryType(Item.InventoryType.FINGER);
        item.setItemLevel(30);
        item.setRequiredLevel(10);
        item.setUniqueEquipped(false);
        entityManager.persist(item);
        return item;
    }

    private void callService(WowCharacterEntity entity) {
        service.getEquipmentForCharacter(entity);
        entityManager.flush();
        entityManager.clear(); // detaches the entity, clears it from Hibernate's cache
    }

    private WowCharacterEntity setUpCharacterInDb(Map<Equipment.Slot, EquipmentEntity.EquippedItem> items) {
        WowCharacterEntity character = new WowCharacterEntity();
        WowCharacterEntity.CompositeKey charKey =
                new WowCharacterEntity.CompositeKey(1L, "soulseeker", 1L);
        character.setPk(charKey);
        character.setName("Thelamar");
        character.setRace(WowCharacter.Race.HUMAN);
        character.setWowClass(WowCharacter.WowClass.PALADIN);
        character.setRealmName("Soulseeker");
        EquipmentEntity equipment = new EquipmentEntity();
        equipment.setItems(items);
        equipment.setCharacter(character);
        character.setEquipment(equipment);

        entityManager.persist(character);
        entityManager.flush();

        return character;
    }
}
