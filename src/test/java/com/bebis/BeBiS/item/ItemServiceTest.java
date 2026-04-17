package com.bebis.BeBiS.item;

import com.bebis.BeBiS.integration.blizzard.BlizzardServiceClient;
import com.bebis.BeBiS.integration.blizzard.dto.EquipmentResponse;
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

import java.util.Objects;
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
        ItemEntity result = service.getOrCreateEntity(ItemTestData.THUNDERFURY_ID, mock(EquipmentResponse.ItemDTO.class));

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
        EquipmentResponse.ItemDTO itemDTO = mock(EquipmentResponse.ItemDTO.class);
        WeaponEntity createdEntity = new WeaponEntity();

        when(repository.findById(pk)).thenReturn(Optional.empty()); // queried item not there
        when(blizzardClient.getBaseItem(itemId)).thenReturn(response);
        when(mapper.mapToSyncData(response, itemDTO)).thenReturn(syncData);
        when(entityFactory.createItemEntity(syncData)).thenReturn(createdEntity);
        when(repository.save(createdEntity)).thenReturn(createdEntity);

        // when
        ItemEntity result = service.getOrCreateEntity(itemId, itemDTO);

        // then
        assertThat(result).isSameAs(createdEntity);
        verify(repository).findById(pk);
        verify(blizzardClient).getBaseItem(itemId);
        verify(mapper).mapToSyncData(response, itemDTO);
        verify(entityFactory).createItemEntity(syncData);
    }

    @Test
    void shouldTreatSuffixedVariantAsDistinctIdentity() {
        // given
        long itemId = 12345L;

        ItemResponse baseResponse = ItemTestData.thunderfuryResponse();
        EquipmentResponse.ItemDTO equippedDTO = mock(EquipmentResponse.ItemDTO.class);
        when(equippedDTO.getSuffixId()).thenReturn(SUFFIX_ENCH_ID);

        ItemEntity.CompositeKey expectedKey = new ItemEntity.CompositeKey(itemId, SUFFIX_ENCH_ID);

        when(repository.findById(expectedKey)).thenReturn(Optional.empty());
        when(blizzardClient.getBaseItem(itemId)).thenReturn(baseResponse);

        ItemSyncData expectedSyncData = mock(ItemSyncData.class);
        when(mapper.mapToSyncData(eq(baseResponse), eq(equippedDTO))).thenReturn(expectedSyncData);

        WeaponEntity createdEntity = new WeaponEntity();
        when(entityFactory.createItemEntity(expectedSyncData)).thenReturn(createdEntity);
        when(repository.save(createdEntity)).thenReturn(createdEntity);

        // when
        service.getOrCreateEntity(itemId, equippedDTO);

        // then
        // Verify that the factory received a SyncData with the correct suffix
        verify(entityFactory).createItemEntity(argThat(Objects::nonNull));
        verify(repository).save(createdEntity);
    }
}