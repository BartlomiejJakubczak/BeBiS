package com.bebis.BeBiS.item;

import com.bebis.BeBiS.integration.blizzard.BlizzardServiceClient;
import com.bebis.BeBiS.integration.blizzard.dto.ItemResponse;
import com.bebis.BeBiS.item.dto.ItemSyncData;
import com.bebis.BeBiS.item.jpa.ItemEntity;
import com.bebis.BeBiS.item.jpa.ItemEntityFactory;
import com.bebis.BeBiS.item.jpa.ItemRepository;
import com.bebis.BeBiS.item.jpa.WeaponEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private BlizzardServiceClient blizzardClient;
    @Mock
    private ItemRepository repository;
    @Mock
    private ItemEntityFactory entityFactory;
    @Mock
    private ItemMapper mapper;

    private ItemService service;

    private static final long BASE_ENCH_ID = 0L;
    private static final long SUFFIX_ENCH_ID = 37L; // "of the Tiger", etc.

    @BeforeEach
    void setup() {
        service = new ItemService(blizzardClient, repository, mapper, entityFactory);
    }

    @Test
    void shouldReturnExistingEntityDirectlyFromRepo() {
        // given
        ItemEntity.CompositeKey pk = new ItemEntity.CompositeKey(ItemTestData.THUNDERFURY_ID, BASE_ENCH_ID);
        WeaponEntity existingWeapon = new WeaponEntity();

        when(repository.findById(pk)).thenReturn(Optional.of(existingWeapon));

        // when
        ItemEntity result = service.getOrCreateEntity(ItemTestData.THUNDERFURY_ID, BASE_ENCH_ID);

        // then
        assertThat(result).isSameAs(existingWeapon);
        verify(repository, times(1)).findById(pk);
        verifyNoInteractions(blizzardClient, mapper, entityFactory);
    }

    @Test
    void shouldOrchestrateFullFetchAndCreationWhenMissing() {
        // given
        long itemId = 12345L;
        ItemEntity.CompositeKey pk = new ItemEntity.CompositeKey(itemId, BASE_ENCH_ID);

        ItemSyncData syncData = mock(ItemSyncData.class);
        ItemResponse response = mock(ItemResponse.class);
        WeaponEntity createdEntity = new WeaponEntity();

        when(repository.findById(pk)).thenReturn(Optional.empty()); // queried item not there
        when(blizzardClient.getBaseItem(itemId)).thenReturn(response);
        when(mapper.mapToSyncData(response, BASE_ENCH_ID)).thenReturn(syncData);
        when(entityFactory.createItemEntity(syncData)).thenReturn(createdEntity);

        // when
        ItemEntity result = service.getOrCreateEntity(itemId, BASE_ENCH_ID);

        // then
        assertThat(result).isSameAs(createdEntity);
        verify(repository).findById(pk);
        verify(blizzardClient).getBaseItem(itemId);
        verify(mapper).mapToSyncData(response, BASE_ENCH_ID);
        verify(entityFactory).createItemEntity(syncData);
    }

    @Test
    void shouldFetchNewVariantEvenIfBaseItemExistsInRepo() {
        // given
        long itemId = 12345L;
        ItemEntity.CompositeKey requestedPk = new ItemEntity.CompositeKey(itemId, SUFFIX_ENCH_ID);

        ItemSyncData syncData = mock(ItemSyncData.class);
        ItemResponse response = mock(ItemResponse.class);
        WeaponEntity createdEntity = new WeaponEntity();

        when(repository.findById(requestedPk)).thenReturn(Optional.empty()); // 21, 37 not in db
        when(blizzardClient.getBaseItem(itemId)).thenReturn(response);
        when(mapper.mapToSyncData(response, SUFFIX_ENCH_ID)).thenReturn(syncData);
        when(entityFactory.createItemEntity(syncData)).thenReturn(createdEntity);

        // when
        ItemEntity result = service.getOrCreateEntity(itemId, SUFFIX_ENCH_ID);

        // then
        assertThat(result).isSameAs(createdEntity);
        verify(repository).findById(requestedPk);
        verify(blizzardClient).getBaseItem(itemId);
        verify(mapper).mapToSyncData(response, SUFFIX_ENCH_ID);
        verify(entityFactory).createItemEntity(syncData);
    }
}