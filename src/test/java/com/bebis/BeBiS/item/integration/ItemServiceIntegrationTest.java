package com.bebis.BeBiS.item.integration;

import com.bebis.BeBiS.base.BaseFullStackTest;
import com.bebis.BeBiS.equipment.EquipmentTestData;
import com.bebis.BeBiS.integration.blizzard.BlizzardServiceClient;
import com.bebis.BeBiS.integration.blizzard.dto.EquipmentResponse;
import com.bebis.BeBiS.integration.blizzard.dto.ItemResponse;
import com.bebis.BeBiS.item.BlizzardItemFetcher;
import com.bebis.BeBiS.item.ItemService;
import com.bebis.BeBiS.item.ItemTestData;
import com.bebis.BeBiS.item.jpa.ItemEntity;
import com.bebis.BeBiS.item.jpa.WeaponEntity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

public class ItemServiceIntegrationTest extends BaseFullStackTest {

    @Autowired
    private ItemService itemService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private BlizzardServiceClient blizzardClient;

    @MockitoSpyBean // a wrapper around the real object, will retain its behavior
    private BlizzardItemFetcher itemFetcher;

    @Test
    void shouldFetchFromBlizzardAndPersistEntityWhenNotInRepo() {
        // given
        ItemResponse response = ItemTestData.thunderfuryResponse();
        long baseId = response.id();
        long suffixId = 0L;

        when(blizzardClient.getBaseItem(baseId)).thenReturn(response);

        Integer countBefore = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM items WHERE base_id = ? AND suffix_id = ?",
                Integer.class, baseId, suffixId);
        assertThat(countBefore).isEqualTo(0);

        EquipmentResponse.ItemDTO dto = EquipmentTestData.fromItemResponseNoSuffix(response, "MAIN_HAND", List.of());

        // when
        Map<EquipmentResponse.ItemDTO, ItemEntity> result = itemService.resolveItems(List.of(dto));

        entityManager.flush(); // tells Hibernate to send SQL to Postgres immediately, without adhering to Transactional

        // then
        assertNotNull(result.get(dto));
        ItemEntity itemEntity = result.get(dto);
        assertThat(itemEntity.getPk().getBaseId()).isEqualTo(baseId);
        assertThat(itemEntity.getPk().getSuffixId()).isEqualTo(suffixId);

        Map<String, Object> dbRow = jdbcTemplate.queryForMap(
                "SELECT name, item_level FROM items WHERE base_id = ? AND suffix_id = ?",
                baseId, suffixId);
        assertThat(dbRow.get("name")).isEqualTo(itemEntity.getName());
        assertThat(dbRow.get("item_level")).isEqualTo(itemEntity.getItemLevel());
    }

    @Test
    void shouldGetFromRepoWhenEntityExists() {
        // given
        ItemResponse response = ItemTestData.thunderfuryResponse();
        long baseId = response.id();
        long suffixId = 0L;
        int itemLevel = response.itemLevel();

        jdbcTemplate.update(
                "INSERT INTO items (base_id, suffix_id, name, item_level, quality, inventory_type, item_category) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                baseId, suffixId, response.name(), itemLevel, "LEGENDARY", "WEAPON", "WEAPON"
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
        assertThat(tfEntity.getPk().getBaseId()).isEqualTo(baseId);
        assertThat(tfEntity.getPk().getSuffixId()).isEqualTo(suffixId);
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
                "SELECT count(*) FROM items WHERE base_id = ? AND suffix_id IN (?, ?)",
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
        verify(blizzardClient, times(1)).getBaseItem(itemId); // should pull from cache the second time
        List<Long> savedSuffixes = jdbcTemplate.queryForList(
                "SELECT suffix_id FROM items WHERE base_id = ?",
                Long.class, itemId);
        assertThat(savedSuffixes).containsExactlyInAnyOrder(0L, enchId);
        String savedName = jdbcTemplate.queryForObject(
                "SELECT name FROM items WHERE base_id = ? AND suffix_id = ?",
                String.class, itemId, enchId);
        assertThat(savedName).contains("of The Bear");
    }

    @Test
    void shouldPersistCacheItemInRedis() {
        // given
        ItemResponse response = ItemTestData.thunderfuryResponse();
        long itemId = response.id();

        when(blizzardClient.getBaseItem(itemId)).thenReturn(response);

        EquipmentResponse.ItemDTO dto = EquipmentTestData.fromItemResponseNoSuffix(response, "MAIN_HAND", List.of());

        // when
        itemService.resolveItems(List.of(dto));

        // then
        Cache itemsCache = cacheManager.getCache("items");
        assertNotNull(itemsCache, "Cache 'items' should exist");

        Cache.ValueWrapper wrapper = itemsCache.get(itemId);
        assertNotNull(wrapper, "Item should be cached in Redis");

        ItemResponse cachedValue = (ItemResponse) wrapper.get();
        assertEquals(response.name(), cachedValue.name(), "Cached data should match original");
    }
}
