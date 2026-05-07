package com.bebis.BeBiS.equipment.integration;

import com.bebis.BeBiS.base.BaseIntegrationTest;
import com.bebis.BeBiS.equipment.Equipment;
import com.bebis.BeBiS.equipment.EquipmentService;
import com.bebis.BeBiS.equipment.EquipmentTestData;
import com.bebis.BeBiS.equipment.jpa.EquipmentEntity;
import com.bebis.BeBiS.integration.blizzard.BlizzardServiceClient;
import com.bebis.BeBiS.integration.blizzard.BlizzardUserClient;
import com.bebis.BeBiS.integration.blizzard.dto.EquipmentResponse;
import com.bebis.BeBiS.integration.blizzard.dto.ItemResponse;
import com.bebis.BeBiS.item.Item;
import com.bebis.BeBiS.item.ItemTestData;
import com.bebis.BeBiS.item.jpa.EquippableItemEntity;
import com.bebis.BeBiS.item.jpa.ItemEntity;
import com.bebis.BeBiS.profile.WowCharacter;
import com.bebis.BeBiS.profile.jpa.WowCharacterEntity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
@TestPropertySource(properties = {"spring.jpa.hibernate.ddl-auto=validate"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // replace H2 from DataJpaTest with postgre
@Transactional // this makes sure that jdbctemplate updates are rolled back after each test
public class EquipmentIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private EquipmentService service;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private BlizzardUserClient blizzardUserClient;

    @MockitoBean
    private BlizzardServiceClient blizzardServiceClient;

    @Test
    void shouldMapEquipmentToCorrectSlots() {
        // given
        long charId = 1L;
        long blizzAccountId = 2L;
        String realmSlug = "soulseeker";
        String charName = "Arthass";

        // put characterEntity into db
        WowCharacterEntity.CompositeKey charKey = setUpCharacterInDb(charId, realmSlug, blizzAccountId, charName, Map.of());

        ItemResponse tf = ItemTestData.thunderfuryResponse();
        EquipmentResponse.ItemDTO dto = EquipmentTestData.fromItemResponseNoSuffix(tf, "main_hand", List.of());
        EquipmentResponse eqResponse = new EquipmentResponse(List.of(dto));

        when(blizzardUserClient.getCharacterEquipment(eq(realmSlug), eq(charName))).thenReturn(eqResponse);
        when(blizzardServiceClient.getBaseItem(tf.id())).thenReturn(tf);

        // when
        callService(charKey.getId(), realmSlug, charKey.getBlizzardAccountId());

        // then
        WowCharacterEntity persistedEntity = entityManager.find(WowCharacterEntity.class, charKey);
        Map<Equipment.Slot, EquipmentEntity.EquippedItem> items = persistedEntity.getEquipment().getItems();

        assertThat(items).hasSize(1);
        assertThat(items).containsKey(Equipment.Slot.MAIN_HAND);

        verify(blizzardServiceClient).getBaseItem(tf.id());
        verify(blizzardUserClient).getCharacterEquipment(eq(realmSlug), eq(charName));
    }

    @Test
    void shouldSkipBadSlotButPersistTheRestOfEquipment() {
        // given
        long charId = 1L;
        long blizzAccountId = 2L;
        String realmSlug = "soulseeker";
        String charName = "Arthass";

        WowCharacterEntity.CompositeKey charKey = setUpCharacterInDb(charId, realmSlug, blizzAccountId, charName, Map.of());

        ItemResponse tf = ItemTestData.thunderfuryResponse();
        EquipmentResponse.ItemDTO wrongDto = EquipmentTestData.fromItemResponseNoSuffix(tf, "left_hand", List.of());

        ItemResponse ring = ItemTestData.equippableItemResponse(1L, "Greatseal", "finger", null);
        EquipmentResponse.ItemDTO ringDto = EquipmentTestData.fromItemResponseNoSuffix(ring, "finger_1", List.of());

        EquipmentResponse eqResponse = new EquipmentResponse(List.of(wrongDto, ringDto));

        when(blizzardUserClient.getCharacterEquipment(eq(realmSlug), eq(charName))).thenReturn(eqResponse);
        when(blizzardServiceClient.getBaseItem(ring.id())).thenReturn(ring);
        when(blizzardServiceClient.getBaseItem(tf.id())).thenReturn(tf);

        // when
        callService(charKey.getId(), realmSlug, charKey.getBlizzardAccountId());

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
        long charId = 1L;
        long blizzAccountId = 2L;
        String realmSlug = "soulseeker";
        String charName = "Arthass";

        WowCharacterEntity.CompositeKey charKey = setUpCharacterInDb(charId, realmSlug, blizzAccountId, charName, Map.of());

        ItemResponse tf = ItemTestData.thunderfuryResponse();
        String enchantName = "Crusader";
        EquipmentResponse.ItemDTO dto = EquipmentTestData.fromItemResponseNoSuffix(tf,
                "main_hand",
                List.of(EquipmentTestData.enchant(1L, enchantName))
        );
        EquipmentResponse eqResponse = new EquipmentResponse(List.of(dto));

        when(blizzardUserClient.getCharacterEquipment(eq(realmSlug), eq(charName))).thenReturn(eqResponse);
        when(blizzardServiceClient.getBaseItem(tf.id())).thenReturn(tf);

        // when
        callService(charKey.getId(), realmSlug, charKey.getBlizzardAccountId());

        // then
        Map<Equipment.Slot, EquipmentEntity.EquippedItem> items = entityManager
                .find(WowCharacterEntity.class, charKey)
                .getEquipment()
                .getItems();

        assertThat(items).hasSize(1);
        assertThat(items).containsKey(Equipment.Slot.MAIN_HAND);
        assertThat(items.get(Equipment.Slot.MAIN_HAND).getPlayerEnchants()).containsExactly(enchantName);

        verify(blizzardServiceClient).getBaseItem(tf.id());
        verify(blizzardUserClient).getCharacterEquipment(eq(realmSlug), eq(charName));
    }

    @Test
    void shouldClearOldEquippedItemsFromDbWhenReplaced() {
        // given
        long charId = 1L;
        long blizzAccountId = 2L;
        String realmSlug = "soulseeker";
        String charName = "Arthass";

        // put a character with previously snapshot items
        Map<Equipment.Slot, EquipmentEntity.EquippedItem> previousSnapshot = new HashMap<>();
        previousSnapshot.put(Equipment.Slot.FINGER_1, setUpEquippedItem(123L, "Ring of the Past"));

        WowCharacterEntity.CompositeKey charKey = setUpCharacterInDb(charId, realmSlug, blizzAccountId, charName, previousSnapshot);

        String newItemName = "Greatseal";
        ItemResponse ring = ItemTestData.equippableItemResponse(1L, newItemName, "finger", null);
        EquipmentResponse.ItemDTO ringDto = EquipmentTestData.fromItemResponseNoSuffix(ring, "finger_1", List.of());
        EquipmentResponse eqResponse = new EquipmentResponse(List.of(ringDto));

        when(blizzardUserClient.getCharacterEquipment(eq(realmSlug), eq(charName))).thenReturn(eqResponse);
        when(blizzardServiceClient.getBaseItem(ring.id())).thenReturn(ring);

        // when
        callService(charKey.getId(), realmSlug, charKey.getBlizzardAccountId());

        // then
        Map<Equipment.Slot, EquipmentEntity.EquippedItem> items = entityManager
                .find(WowCharacterEntity.class, charKey)
                .getEquipment()
                .getItems();

        assertThat(items).hasSize(1);
        assertThat(items).containsKey(Equipment.Slot.FINGER_1);
        assertThat(items.get(Equipment.Slot.FINGER_1).getItem().getName()).isEqualTo(newItemName);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM equipped_items WHERE item_id = ? AND random_enchantment_id = ?",
                Integer.class, 123L, 0L);

        assertThat(count).isEqualTo(0);

        verify(blizzardServiceClient).getBaseItem(ring.id());
        verify(blizzardUserClient).getCharacterEquipment(eq(realmSlug), eq(charName));
    }

    @Test
    void shouldClearOldEquippedItemsFromDbWhenEmptyResponse() {
        // given
        long charId = 1L;
        long blizzAccountId = 2L;
        String realmSlug = "soulseeker";
        String charName = "Arthass";

        // put a character with previously snapshot items
        Map<Equipment.Slot, EquipmentEntity.EquippedItem> previousSnapshot = new HashMap<>();
        previousSnapshot.put(Equipment.Slot.FINGER_1, setUpEquippedItem(123L, "Ring of the Past"));
        previousSnapshot.put(Equipment.Slot.FINGER_2, setUpEquippedItem(456L, "Ring of the Future"));

        WowCharacterEntity.CompositeKey charKey = setUpCharacterInDb(charId, realmSlug, blizzAccountId, charName, previousSnapshot);

        when(blizzardUserClient.getCharacterEquipment(eq(realmSlug), eq(charName))).thenReturn(new EquipmentResponse(List.of()));

        // when
        callService(charKey.getId(), realmSlug, charKey.getBlizzardAccountId());

        // then
        Map<Equipment.Slot, EquipmentEntity.EquippedItem> items = entityManager
                .find(WowCharacterEntity.class, charKey)
                .getEquipment()
                .getItems();
        assertThat(items).isEmpty();

        List<Long> savedEquippedItems = jdbcTemplate.queryForList("SELECT id FROM equipped_items WHERE item_id IN (?, ?)",
                Long.class, 123L, 456L);
        assertThat(savedEquippedItems).isEmpty();

        verifyNoInteractions(blizzardServiceClient);
        verify(blizzardUserClient).getCharacterEquipment(eq(realmSlug), eq(charName));
    }

    @Test
    void shouldPullBaseDataOfEquippedItemFromDbWhenItemExists() {
        // given
        long charId = 1L;
        long blizzAccountId = 2L;
        String realmSlug = "soulseeker";
        String charName = "Arthass";

        WowCharacterEntity.CompositeKey charKey = setUpCharacterInDb(charId, realmSlug, blizzAccountId, charName, Map.of());
        ItemEntity item = setUpItemInDb(charId, "Greatseal");
        ItemResponse itemResponse = ItemTestData.equippableItemResponse(item.getId().getItemId(), item.getName(), "finger", null);
        EquipmentResponse.ItemDTO itemDTO = EquipmentTestData.fromItemResponseNoSuffix(itemResponse, "finger_1", List.of());

        when(blizzardUserClient.getCharacterEquipment(eq(realmSlug), eq(charName))).thenReturn(new EquipmentResponse(List.of(itemDTO)));

        // when
        callService(charKey.getId(), realmSlug, charKey.getBlizzardAccountId());

        // then
        verifyNoInteractions(blizzardServiceClient); // no need to pull data from blizzard for item that exists in db
    }

    private EquipmentEntity.EquippedItem setUpEquippedItem(long id, String name) {
        ItemEntity item = setUpItemInDb(id, name);
        EquipmentEntity.EquippedItem previousEqItem = new EquipmentEntity.EquippedItem();
        previousEqItem.setItem(item);
        return previousEqItem;
    }

    private ItemEntity setUpItemInDb(long id, String name) {
        ItemEntity item = new EquippableItemEntity();
        item.setId(new ItemEntity.CompositeKey(id, 0L));
        item.setName(name);
        item.setQuality(Item.Quality.UNCOMMON);
        item.setInventoryType(Item.InventoryType.FINGER);
        item.setItemLevel(30);
        item.setRequiredLevel(10);
        item.setUniqueEquipped(false);
        entityManager.persist(item);
        return item;
    }

    private void callService(long charId, String realmSlug, long blizzAccountId) {
        service.getEquipmentForCharacter(charId, realmSlug, blizzAccountId);
        entityManager.flush();
    }

    private WowCharacterEntity.CompositeKey setUpCharacterInDb(
            long charId,
            String realmSlug,
            long blizzAccountId,
            String charName,
            Map<Equipment.Slot, EquipmentEntity.EquippedItem> items
    ) {
        WowCharacterEntity character = new WowCharacterEntity();
        WowCharacterEntity.CompositeKey charKey =
                new WowCharacterEntity.CompositeKey(charId, realmSlug, blizzAccountId);
        character.setPk(charKey);
        character.setName(charName);
        character.setRace(WowCharacter.Race.HUMAN);
        character.setWowClass(WowCharacter.WowClass.PALADIN);
        character.setRealmName("Soulseeker");
        EquipmentEntity equipment = new EquipmentEntity();
        equipment.setItems(items);
        equipment.setCharacter(character);
        character.setEquipment(equipment);
        entityManager.persist(character);

        // Clear ensures the Service has to actually fetch it from the DB.
        entityManager.flush();
        entityManager.clear();

        return charKey;
    }
}
