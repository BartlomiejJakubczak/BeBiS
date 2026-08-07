package com.bebis.BeBiS.item;

import com.bebis.BeBiS.base.BaseFullStackTest;
import com.bebis.BeBiS.equipment.EquipmentTestData;
import com.bebis.BeBiS.integration.blizzard.dto.EquipmentResponse;
import com.bebis.BeBiS.integration.blizzard.dto.ItemResponse;
import com.bebis.BeBiS.item.event.ItemPersistedEvent;
import com.bebis.BeBiS.item.jpa.ItemEntity;
import com.bebis.BeBiS.item.jpa.ItemRepository;
import com.bebis.BeBiS.item.jpa.WeaponEntity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RecordApplicationEvents
public class ItemServiceIT extends BaseFullStackTest {

    @Autowired
    private ItemService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ApplicationEvents applicationEvents;

    @MockitoSpyBean
    private BlizzardItemFetcher itemFetcher;

    @MockitoSpyBean
    private ItemRepository repository;

    @Captor
    private ArgumentCaptor<ItemEntity> saveCaptor;

    @Test
    void shouldFetchFromBlizzardAndPersistEntityWhenNotInRepo() {
        // given
        ItemResponse response = ItemResponseBuilder.newWeaponInstance().build();
        long baseId = response.id();
        long suffixId = 0L;

        when(blizzardServiceClient.getBaseItem(baseId)).thenReturn(response);

        assertThatItemNotInDb(baseId, suffixId);

        EquipmentResponse.ItemDTO dto = EquipmentTestData.fromItemResponseNoSuffix(response, "MAIN_HAND", List.of());

        // when
        Map<EquipmentResponse.ItemDTO, ItemEntity> result = callServiceForDbChecks(dto);

        // then
        assertThat(result.get(dto)).isNotNull();
        ItemEntity itemEntity = result.get(dto);
        assertThat(itemEntity.getPk().getBaseId()).isEqualTo(baseId);
        assertThat(itemEntity.getPk().getSuffixId()).isEqualTo(suffixId);

        Map<String, Object> dbRow = jdbcTemplate.queryForMap(
                "SELECT name, item_level FROM items WHERE base_id = ? AND suffix_id = ?",
                baseId, suffixId);
        assertThat(dbRow.get("name")).isEqualTo(itemEntity.getName());
        assertThat(dbRow.get("item_level")).isEqualTo(itemEntity.getItemLevel());

        verify(itemFetcher).fetchItem(baseId);
        verify(blizzardServiceClient).getBaseItem(baseId);

        verify(repository).save(saveCaptor.capture());
        assertThat(saveCaptor.getValue()).isEqualTo(result.get(dto));

        assertItemPersistedEventFired(1);
    }

    @Test
    void shouldGetFromRepoWhenEntityExists() {
        // given
        ItemResponse response = ItemResponseBuilder.newWeaponInstance().build();
        long baseId = response.id();
        long suffixId = 0L;
        int itemLevel = response.itemLevel();

        jdbcTemplate.update(
                "INSERT INTO items (base_id, suffix_id, name, item_level, quality, inventory_type, item_category) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                baseId, suffixId, response.name(), itemLevel, response.quality().type().toUpperCase(),
                response.inventoryType().type().toUpperCase(), "WEAPON"
        );

        EquipmentResponse.ItemDTO dto = EquipmentTestData.fromItemResponseNoSuffix(response, "MAIN_HAND", List.of());

        // when
        Map<EquipmentResponse.ItemDTO, ItemEntity> result = service.resolveItems(List.of(dto));

        // then
        assertThat(result.get(dto)).isNotNull();
        assertThat(result.get(dto)).isInstanceOf(WeaponEntity.class); // check for correctness of polymorphism of ItemEntity
        ItemEntity tfEntity = result.get(dto);
        // check if the pk was mapped correctly
        assertThat(tfEntity.getPk().getBaseId()).isEqualTo(baseId);
        assertThat(tfEntity.getPk().getSuffixId()).isEqualTo(suffixId);
        // check if some of other entity's fields were populated correctly
        assertThat(tfEntity.getName()).isEqualTo(response.name());
        assertThat(tfEntity.getItemLevel()).isEqualTo(itemLevel);

        verifyNoInteractions(itemFetcher);
        verifyNoInteractions(blizzardServiceClient);

        verify(repository, never()).save(any());
        assertItemPersistedEventFired(0);
    }

    @Test
    void shouldFetchTwoDifferentItemsSameItemId() {
        // given
        ItemResponse response = ItemResponseBuilder.newEquippableInstance().build();
        long baseId = response.id();
        long suffixId = 37L;

        when(blizzardServiceClient.getBaseItem(baseId)).thenReturn(response);

        assertThatItemNotInDb(baseId, suffixId);

        EquipmentResponse.ItemDTO dto = EquipmentTestData.fromItemResponseNoSuffix(response, "FINGER_1", List.of());
        EquipmentResponse.ItemDTO suffixedDTO = EquipmentTestData.fromItemResponseSuffixed(
                response, "FINGER_2", "RARE", "of The Bear", suffixId, response.itemLevel() + 10,
                List.of(EquipmentTestData.stat("STRENGTH", 9), EquipmentTestData.stat("STAMINA", 9)),
                List.of()
        );

        // when
        Map<EquipmentResponse.ItemDTO, ItemEntity> result = callServiceForDbChecks(dto, suffixedDTO);
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.get(dto)).isNotNull();
        assertThat(result.get(suffixedDTO)).isNotNull();

        // then
        List<Long> savedSuffixes = jdbcTemplate.queryForList(
                "SELECT suffix_id FROM items WHERE base_id = ?",
                Long.class, baseId);
        assertThat(savedSuffixes).containsExactlyInAnyOrder(0L, suffixId);

        String savedName = jdbcTemplate.queryForObject(
                "SELECT name FROM items WHERE base_id = ? AND suffix_id = ?",
                String.class, baseId, suffixId);
        assertThat(savedName).contains("of The Bear");

        verify(itemFetcher).fetchItem(eq(baseId)); // the second time was pulled from the cache
        verify(blizzardServiceClient, times(1)).getBaseItem(baseId); // should pull from cache the second time

        verify(repository, times(2)).save(saveCaptor.capture());
        assertThat(saveCaptor.getAllValues())
                .containsExactlyInAnyOrder(result.get(dto), result.get(suffixedDTO));

        assertItemPersistedEventFired(2);
    }

    @Test
    void shouldIgnoreInvalidItemWhenResolving() {
        // given
        ItemResponse response = ItemResponseBuilder.newEquippableInstance()
                .withId(1L)
                .build();
        ItemResponse brokenResponse = ItemResponseBuilder.newArmorInstance()
                .withId(2L)
                .withName(null) // null name
                .build();

        when(blizzardServiceClient.getBaseItem(response.id())).thenReturn(response);
        when(blizzardServiceClient.getBaseItem(brokenResponse.id())).thenReturn(brokenResponse);

        EquipmentResponse.ItemDTO goodResponseDTO = EquipmentTestData.fromItemResponseNoSuffix(response, "FINGER_1", List.of());
        EquipmentResponse.ItemDTO brokenResponseDTO = EquipmentTestData.fromItemResponseNoSuffix(brokenResponse, "CHEST", List.of());

        // when
        Map<EquipmentResponse.ItemDTO, ItemEntity> result = service.resolveItems(List.of(goodResponseDTO, brokenResponseDTO));

        // then
        assertThat(result.get(goodResponseDTO)).isNotNull();
        assertThat(result.get(brokenResponseDTO)).isNull();
        assertItemPersistedEventFired(1);
    }

    @Test
    void shouldHandleGettingItemsFromBothDbAndBlizzardSimultaneously() {
        // given
        ItemResponse responseFromDb = ItemResponseBuilder.newWeaponInstance().build();

        // save tf into db
        jdbcTemplate.update(
                "INSERT INTO items (base_id, suffix_id, name, item_level, quality, inventory_type, item_category) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                responseFromDb.id(), 0L, responseFromDb.name(), responseFromDb.itemLevel(), responseFromDb.quality().type().toUpperCase(),
                responseFromDb.inventoryType().type().toUpperCase(), "WEAPON"
        );

        ItemResponse responseFromBlizz = ItemResponseBuilder.newEquippableInstance().build();

        EquipmentResponse.ItemDTO dbDTO = EquipmentTestData.fromItemResponseNoSuffix(responseFromDb, "MAIN_HAND", List.of());
        EquipmentResponse.ItemDTO blizzDTO = EquipmentTestData.fromItemResponseNoSuffix(responseFromBlizz, "FINGER_1", List.of());

        when(blizzardServiceClient.getBaseItem(responseFromBlizz.id())).thenReturn(responseFromBlizz);

        // when
        Map<EquipmentResponse.ItemDTO, ItemEntity> result = callServiceForDbChecks(dbDTO, blizzDTO);

        // then
        assertThat(result.size()).isEqualTo(2);
        assertThat(result).containsKeys(dbDTO, blizzDTO);

        verify(itemFetcher).fetchItem(eq(responseFromBlizz.id()));
        verify(blizzardServiceClient).getBaseItem(responseFromBlizz.id());

        verify(repository).save(saveCaptor.capture());
        assertThat(saveCaptor.getValue()).isEqualTo(result.get(blizzDTO));

        assertItemPersistedEventFired(1);
    }

    @Test
    void shouldHandleDuplicateItems_NotUniqueEquipped() {
        // given
        ItemResponse response = ItemResponseBuilder.newEquippableInstance().build();
        when(blizzardServiceClient.getBaseItem(response.id())).thenReturn(response);

        EquipmentResponse.ItemDTO ring1DTO = EquipmentTestData.fromItemResponseSuffixed(response, "FINGER_1", "RARE",
                "of The Bear", 37L, 60,
                List.of(EquipmentTestData.stat("STRENGTH", 9), EquipmentTestData.stat("STAMINA", 9)), List.of());

        EquipmentResponse.ItemDTO ring2DTO = EquipmentTestData.fromItemResponseSuffixed(response, "FINGER_2", "RARE",
                "of The Bear", 37L, 60,
                List.of(EquipmentTestData.stat("STRENGTH", 9), EquipmentTestData.stat("STAMINA", 9)), List.of());

        // when
        Map<EquipmentResponse.ItemDTO, ItemEntity> result = callServiceForDbChecks(ring1DTO, ring2DTO);

        // then
        assertThat(result.size()).isEqualTo(2);

        assertThat(result.get(ring1DTO)).isNotNull();
        assertThat(result.get(ring2DTO)).isNotNull();

        assertThat(result.get(ring1DTO)).isEqualTo(result.get(ring2DTO)); // both dtos point to the same entity

        verify(itemFetcher).fetchItem(eq(response.id()));
        verify(blizzardServiceClient).getBaseItem(response.id());

        verify(repository, times(1)).save(any());
        assertItemPersistedEventFired(1);

        Integer savedCount = jdbcTemplate.queryForObject("SELECT COUNT(base_id) FROM items WHERE base_id = ?", Integer.class, response.id());
        assertThat(savedCount).isEqualTo(1);
    }

    @Test
    void shouldHandleExternalFetcherFailures() {
        // given
        ItemResponse response = ItemResponseBuilder.newEquippableInstance().build();
        EquipmentResponse.ItemDTO dto = EquipmentTestData.fromItemResponseNoSuffix(response, "FINGER_1", List.of());

        when(blizzardServiceClient.getBaseItem(response.id())).thenThrow(RestClientException.class); // api is down

        // when
        Map<EquipmentResponse.ItemDTO, ItemEntity> result = service.resolveItems(List.of(dto));

        // then
        assertThat(result).isEmpty(); // invalid item was omitted, exception was caught
    }

    private void assertItemPersistedEventFired(int times) {
        List<ItemPersistedEvent> eventsFired = applicationEvents.stream(ItemPersistedEvent.class).toList();
        assertThat(eventsFired.size()).isEqualTo(times);
    }

    @Test
    void shouldPersistCacheItemInRedis() {
        // given
        ItemResponse response = ItemResponseBuilder.newWeaponInstance().build();
        long itemId = response.id();

        when(blizzardServiceClient.getBaseItem(itemId)).thenReturn(response);

        EquipmentResponse.ItemDTO dto = EquipmentTestData.fromItemResponseNoSuffix(response, "MAIN_HAND", List.of());

        // when
        service.resolveItems(List.of(dto));

        // then
        Cache itemsCache = cacheManager.getCache("items");
        assertThat(itemsCache).isNotNull();

        Cache.ValueWrapper wrapper = itemsCache.get(itemId);
        assertThat(wrapper).isNotNull();

        ItemResponse cachedValue = (ItemResponse) wrapper.get();
        assertThat(response.name()).isEqualTo(cachedValue.name());
    }

    private void assertThatItemNotInDb(long baseId, long suffixId) {
        Integer countBefore = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM items WHERE base_id = ? AND suffix_id IN (?, ?)",
                Integer.class, baseId, suffixId, 0);
        assertThat(countBefore).isEqualTo(0);
    }

    private Map<EquipmentResponse.ItemDTO, ItemEntity> callServiceForDbChecks(EquipmentResponse.ItemDTO... dtos) {
        Map<EquipmentResponse.ItemDTO, ItemEntity> resolved = service.resolveItems(List.of(dtos));
        entityManager.flush(); // make sure hibernate persists the new entries
        return resolved;
    }
}
