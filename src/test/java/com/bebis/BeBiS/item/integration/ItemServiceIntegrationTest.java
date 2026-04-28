package com.bebis.BeBiS.item.integration;

import com.bebis.BeBiS.base.BaseDatabaseTest;
import com.bebis.BeBiS.equipment.EquipmentTestData;
import com.bebis.BeBiS.integration.blizzard.BlizzardServiceClient;
import com.bebis.BeBiS.integration.blizzard.dto.EquipmentResponse;
import com.bebis.BeBiS.integration.blizzard.dto.ItemResponse;
import com.bebis.BeBiS.item.ItemService;
import com.bebis.BeBiS.item.ItemTestData;
import com.bebis.BeBiS.item.jpa.ItemEntity;
import com.bebis.BeBiS.item.jpa.WeaponEntity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@SpringBootTest // this suite will need actual Services, mappers and so on
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop"
}) // to be replaced by a migration tool like Flyway or Liquibase
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // replace H2 from DataJpaTest with postgre
@Transactional // this makes sure that jdbctemplate updates are rolled back after each test
public class ItemServiceIntegrationTest extends BaseDatabaseTest {

    @Autowired
    private ItemService itemService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private BlizzardServiceClient blizzardClient;

    @Test
    void shouldFetchFromBlizzardAndPersistEntityWhenNotInRepo() {
        // given
        ItemResponse response = ItemTestData.thunderfuryResponse();
        long itemId = response.id();
        long enchId = 0L;

        when(blizzardClient.getBaseItem(itemId)).thenReturn(response);

        Integer countBefore = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM items WHERE item_id = ? AND random_enchantment_id = ?",
                Integer.class, itemId, enchId);
        assertThat(countBefore).isEqualTo(0);

        EquipmentResponse.ItemDTO dto = EquipmentTestData.fromItemResponseNoSuffix(response, "MAIN_HAND", List.of());

        // when
        Map<EquipmentResponse.ItemDTO, ItemEntity> result = itemService.resolveItems(List.of(dto));

        entityManager.flush(); // tells Hibernate to send SQL to Postgres immediately, without adhering to Transactional

        // then
        assertNotNull(result.get(dto));
        ItemEntity itemEntity = result.get(dto);
        assertThat(itemEntity.getId().getItemId()).isEqualTo(itemId);
        assertThat(itemEntity.getId().getRandomEnchantmentId()).isEqualTo(enchId);

        Map<String, Object> dbRow = jdbcTemplate.queryForMap(
                "SELECT name, item_level FROM items WHERE item_id = ? AND random_enchantment_id = ?",
                itemId, enchId);
        assertThat(dbRow.get("name")).isEqualTo(itemEntity.getName());
        assertThat(dbRow.get("item_level")).isEqualTo(itemEntity.getItemLevel());
    }

    @Test
    void shouldGetFromRepoWhenEntityExists() {
        // given
        ItemResponse response = ItemTestData.thunderfuryResponse();
        long itemId = response.id();
        long enchId = 0L;
        int itemLevel = response.itemLevel();

        jdbcTemplate.update(
                "INSERT INTO items (item_id, random_enchantment_id, name, item_level, quality, inventory_type, item_category) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                itemId, enchId, response.name(), itemLevel, "LEGENDARY", "WEAPON", "WEAPON"
        );

        EquipmentResponse.ItemDTO dto = EquipmentTestData.fromItemResponseNoSuffix(response, "MAIN_HAND", List.of());

        // when
        Map<EquipmentResponse.ItemDTO, ItemEntity> result = itemService.resolveItems(List.of(dto));

        // then
        verifyNoInteractions(blizzardClient);

        assertNotNull(result.get(dto));
        assertThat(result.get(dto)).isInstanceOf(WeaponEntity.class); // check for correctness of polymorphism of ItemEntity
        ItemEntity tfEntity = result.get(dto);
        // check if the pk was mapped correctly
        assertThat(tfEntity.getId().getItemId()).isEqualTo(itemId);
        assertThat(tfEntity.getId().getRandomEnchantmentId()).isEqualTo(enchId);
        // check if some of other entity's fields were populated correctly
        assertThat(tfEntity.getName()).isEqualTo(response.name());
        assertThat(tfEntity.getItemLevel()).isEqualTo(itemLevel);
    }

    @Test
    void shouldFetchTwoDifferentEntitiesSameItemId() {
        // given
        ItemResponse response = ItemTestData.armorResponse(21L, "Greatseal", 150);
        long itemId = response.id();
        long enchId = 37L;

        when(blizzardClient.getBaseItem(itemId)).thenReturn(response);

        Integer countBefore = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM items WHERE item_id = ? AND random_enchantment_id IN (?, ?)",
                Integer.class, itemId, enchId, 0);
        assertThat(countBefore).isEqualTo(0);

        EquipmentResponse.ItemDTO dto = EquipmentTestData.fromItemResponseNoSuffix(response, "FINGER_1", List.of());
        EquipmentResponse.ItemDTO suffixedDTO = EquipmentTestData.fromItemResponseSuffixed(
                response, "FINGER_2", "RARE", "of The Bear", enchId, response.itemLevel() + 10,
                List.of(EquipmentTestData.stat("STRENGTH", 9), EquipmentTestData.stat("STAMINA", 9)),
                List.of()
        );

        // when
        itemService.resolveItems(List.of(dto, suffixedDTO));

        entityManager.flush(); // make sure hibernate persists the new entries

        // then
        verify(blizzardClient, times(2)).getBaseItem(itemId);
        List<Long> savedSuffixes = jdbcTemplate.queryForList(
                "SELECT random_enchantment_id FROM items WHERE item_id = ?",
                Long.class, itemId);
        assertThat(savedSuffixes).containsExactlyInAnyOrder(0L, enchId);
        String savedName = jdbcTemplate.queryForObject(
                "SELECT name FROM items WHERE item_id = ? AND random_enchantment_id = ?",
                String.class, itemId, enchId);
        assertThat(savedName).contains("of The Bear");
    }
}
