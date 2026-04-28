package com.bebis.BeBiS.item;

import com.bebis.BeBiS.equipment.EquipmentTestData;
import com.bebis.BeBiS.integration.blizzard.BlizzardServiceClient;
import com.bebis.BeBiS.integration.blizzard.dto.EquipmentResponse;
import com.bebis.BeBiS.integration.blizzard.dto.ItemResponse;
import com.bebis.BeBiS.item.dto.ItemSyncData;
import com.bebis.BeBiS.item.jpa.EquippableItemEntity;
import com.bebis.BeBiS.item.jpa.ItemEntity;
import com.bebis.BeBiS.item.jpa.ItemEntityFactory;
import com.bebis.BeBiS.item.jpa.ItemRepository;
import com.bebis.BeBiS.item.jpa.WeaponEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;

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

    @Captor
    ArgumentCaptor<Iterable<ItemEntity.CompositeKey>> captor;

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
        EquipmentResponse.ItemDTO tfDTO = EquipmentTestData.fromItemResponseNoSuffix(
                ItemTestData.thunderfuryResponse(),
                "main_hand",
                List.of()
        );

        ItemEntity.CompositeKey pk = new ItemEntity.CompositeKey(tfDTO.item().id(), BASE_ENCH_ID);
        WeaponEntity existingWeapon = new WeaponEntity();
        existingWeapon.setId(pk);

        when(repository.findAllById(Set.of(pk))).thenReturn(List.of(existingWeapon));
        when(mapper.mapSuffixId(eq(tfDTO))).thenReturn(BASE_ENCH_ID);

        // when
        Map<EquipmentResponse.ItemDTO, ItemEntity> result = service.resolveItems(List.of(tfDTO));

        // then
        assertThat(result).containsEntry(tfDTO, existingWeapon);
        verify(repository, times(1)).findAllById(Set.of(pk));
        verify(mapper, times(2)).mapSuffixId(tfDTO);
        verifyNoInteractions(blizzardClient, entityFactory);
    }

    @Test
    void shouldOrchestrateFullFetchAndCreationWhenMissing() {
        // given
        ItemResponse tfResponse = ItemTestData.thunderfuryResponse();
        EquipmentResponse.ItemDTO tfDTO = EquipmentTestData.fromItemResponseNoSuffix(
                tfResponse,
                "main_hand",
                List.of()
        );

        ItemEntity.CompositeKey pk = new ItemEntity.CompositeKey(tfDTO.item().id(), BASE_ENCH_ID);
        ItemSyncData syncDataMock = mock(ItemSyncData.class);
        WeaponEntity newWeapon = new WeaponEntity();

        when(repository.findAllById(Set.of(pk))).thenReturn(List.of()); // queried item not there

        when(mapper.mapSuffixId(eq(tfDTO))).thenReturn(BASE_ENCH_ID);
        when(blizzardClient.getBaseItem(eq(tfDTO.item().id()))).thenReturn(tfResponse);
        when(mapper.mapToSyncData(eq(tfResponse), eq(tfDTO))).thenReturn(syncDataMock);
        when(entityFactory.createItemEntity(syncDataMock)).thenReturn(newWeapon);
        when(repository.save(newWeapon)).thenReturn(newWeapon);

        // when
        Map<EquipmentResponse.ItemDTO, ItemEntity> result = service.resolveItems(List.of(tfDTO));

        // then
        assertThat(result.get(tfDTO)).isSameAs(newWeapon);
        verify(repository, times(1)).findAllById(Set.of(pk));
        verify(blizzardClient).getBaseItem(tfDTO.item().id());
        verify(entityFactory).createItemEntity(syncDataMock);
        verify(repository).save(newWeapon);
    }

    @Test
    void shouldTreatSuffixedVariantAsDistinctIdentity() {
        // given
        long itemId = 2137L;
        String itemName = "Greatseal";
        long suffixId_1 = SUFFIX_ENCH_ID;
        long suffixId_2 = SUFFIX_ENCH_ID + 1;
        ItemResponse itemResponse = ItemTestData.equippableItemResponse(itemId, itemName, "FINGER", null);
        EquipmentResponse.ItemDTO suffixedItemDTO_1 = EquipmentTestData.fromItemResponseSuffixed(itemResponse, "finger_1", "uncommon",
                "of the Monkey", suffixId_1, 69, List.of(), List.of());
        EquipmentResponse.ItemDTO suffixedItemDTO_2 = EquipmentTestData.fromItemResponseSuffixed(itemResponse, "finger_2", "uncommon",
                "of the Tiger", suffixId_2, 69, List.of(), List.of());

        ItemEntity.CompositeKey pk_1 = new ItemEntity.CompositeKey(itemId, suffixId_1);
        ItemEntity.CompositeKey pk_2 = new ItemEntity.CompositeKey(itemId, suffixId_2);
        ItemSyncData syncDataMock = mock(ItemSyncData.class);
        ItemEntity newItem = new EquippableItemEntity();

        when(repository.findAllById(eq(Set.of(pk_1, pk_2)))).thenReturn(List.of());

        when(mapper.mapSuffixId(eq(suffixedItemDTO_1))).thenReturn(suffixId_1);
        when(mapper.mapSuffixId(eq(suffixedItemDTO_2))).thenReturn(suffixId_2);
        when(blizzardClient.getBaseItem(itemId)).thenReturn(itemResponse);
        when(mapper.mapToSyncData(any(ItemResponse.class), any(EquipmentResponse.ItemDTO.class))).thenReturn(syncDataMock);
        when(entityFactory.createItemEntity(syncDataMock)).thenReturn(newItem);
        when(repository.save(newItem)).thenReturn(newItem);

        // when
        Map<EquipmentResponse.ItemDTO, ItemEntity> result = service.resolveItems(List.of(suffixedItemDTO_1, suffixedItemDTO_2));

        // then
        assertThat(result.size()).isEqualTo(2);

        verify(repository).findAllById(captor.capture());
        assertThat(captor.getValue()).containsExactlyInAnyOrder(pk_1, pk_2);

        verify(blizzardClient, times(2)).getBaseItem(itemId);
        verify(entityFactory, times(2)).createItemEntity(syncDataMock);
        verify(repository, times(2)).save(newItem);
    }

    @Test
    void shouldHandleDuplicateIdenticalItemsWithoutRedundantFetches() {
        // given
        long itemId = 2137L;
        String itemName = "Greatseal";
        String suffixName = "of the Monkey";
        ItemResponse itemResponse = ItemTestData.equippableItemResponse(itemId, itemName, "FINGER", null);
        EquipmentResponse.ItemDTO firstRing = EquipmentTestData.fromItemResponseSuffixed(itemResponse, "finger_1", "uncommon",
                suffixName, SUFFIX_ENCH_ID, 69, List.of(), List.of());
        EquipmentResponse.ItemDTO secondRing = EquipmentTestData.fromItemResponseSuffixed(itemResponse, "finger_2", "uncommon",
                suffixName, SUFFIX_ENCH_ID, 69, List.of(), List.of());

        ItemEntity.CompositeKey firstPk = new ItemEntity.CompositeKey(itemId, SUFFIX_ENCH_ID);
        ItemSyncData syncDataMock = mock(ItemSyncData.class);
        ItemEntity newItem = new EquippableItemEntity();

        when(repository.findAllById(eq(Set.of(firstPk)))).thenReturn(List.of()); // service de-duplicates keys by using map's keyset

        when(mapper.mapSuffixId(eq(firstRing))).thenReturn(SUFFIX_ENCH_ID);
        when(mapper.mapSuffixId(eq(secondRing))).thenReturn(SUFFIX_ENCH_ID);
        when(blizzardClient.getBaseItem(itemId)).thenReturn(itemResponse);
        when(mapper.mapToSyncData(any(ItemResponse.class), any(EquipmentResponse.ItemDTO.class))).thenReturn(syncDataMock);
        when(entityFactory.createItemEntity(syncDataMock)).thenReturn(newItem);
        when(repository.save(newItem)).thenReturn(newItem);

        // when
        Map<EquipmentResponse.ItemDTO, ItemEntity> result = service.resolveItems(List.of(firstRing, secondRing));

        // then
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.get(firstRing)).isSameAs(newItem);
        assertThat(result.get(secondRing)).isSameAs(newItem);
        assertThat(result.keySet()).containsExactlyInAnyOrder(firstRing, secondRing);

        verify(repository).findAllById(captor.capture());
        assertThat(captor.getValue()).hasSize(1).contains(firstPk); // service de-duplicates keys by using map's keyset

        verify(blizzardClient, times(1)).getBaseItem(itemId); // should call blizz once,
        // be saved to db and then pull from db on the second one
        verify(entityFactory, times(1)).createItemEntity(syncDataMock);
        verify(repository, times(1)).save(newItem);
    }

    @Test
    void shouldCorrectlyDelegateApiAndDbCallsWhenItemsInDbAndMissing() {
        // given
        ItemResponse tfResponse = ItemTestData.thunderfuryResponse();
        EquipmentResponse.ItemDTO tfDTO = EquipmentTestData.fromItemResponseNoSuffix(
                tfResponse,
                "main_hand",
                List.of()
        );
        long existingId = tfDTO.item().id();
        long itemId = 2137L;
        String itemName = "Greatseal";
        long suffixId_1 = SUFFIX_ENCH_ID;
        long suffixId_2 = SUFFIX_ENCH_ID + 1;
        ItemResponse itemResponse = ItemTestData.equippableItemResponse(itemId, itemName, "FINGER", null);
        EquipmentResponse.ItemDTO suffixedItemDTO_1 = EquipmentTestData.fromItemResponseSuffixed(itemResponse, "finger_1", "uncommon",
                "of the Monkey", suffixId_1, 69, List.of(), List.of());
        EquipmentResponse.ItemDTO suffixedItemDTO_2 = EquipmentTestData.fromItemResponseSuffixed(itemResponse, "finger_2", "uncommon",
                "of the Tiger", suffixId_2, 69, List.of(), List.of());

        ItemEntity.CompositeKey firstPk = new ItemEntity.CompositeKey(existingId, BASE_ENCH_ID);
        ItemEntity.CompositeKey secondPk = new ItemEntity.CompositeKey(itemId, suffixId_1);
        ItemEntity.CompositeKey thirdPk = new ItemEntity.CompositeKey(itemId, suffixId_2);

        ItemEntity existingTfEntity = new EquippableItemEntity();
        existingTfEntity.setId(firstPk);

        ItemEntity newItem = new EquippableItemEntity();
        ItemSyncData syncDataMock = mock(ItemSyncData.class);

        when(repository.findAllById(eq(Set.of(firstPk, secondPk, thirdPk)))).thenReturn(List.of(existingTfEntity));

        when(mapper.mapSuffixId(eq(tfDTO))).thenReturn(BASE_ENCH_ID);
        when(mapper.mapSuffixId(eq(suffixedItemDTO_1))).thenReturn(suffixId_1);
        when(mapper.mapSuffixId(eq(suffixedItemDTO_2))).thenReturn(suffixId_2);
        when(blizzardClient.getBaseItem(eq(itemId))).thenReturn(itemResponse);
        when(mapper.mapToSyncData(any(ItemResponse.class), any(EquipmentResponse.ItemDTO.class))).thenReturn(syncDataMock);
        when(entityFactory.createItemEntity(syncDataMock)).thenReturn(newItem);
        when(repository.save(newItem)).thenReturn(newItem);

        // when
        Map<EquipmentResponse.ItemDTO, ItemEntity> result = service.resolveItems(List.of(tfDTO, suffixedItemDTO_1, suffixedItemDTO_2));

        //then
        assertThat(result.size()).isEqualTo(3);
        assertThat(result.keySet()).containsExactlyInAnyOrder(tfDTO, suffixedItemDTO_1, suffixedItemDTO_2);

        verify(repository).findAllById(captor.capture());
        assertThat(captor.getValue()).hasSize(3).contains(firstPk, secondPk, thirdPk);

        verify(blizzardClient, times(2)).getBaseItem(itemId);
        verify(blizzardClient, never()).getBaseItem(existingId);
        verify(entityFactory, times(2)).createItemEntity(syncDataMock);
        verify(repository, times(2)).save(any(ItemEntity.class));
    }

}