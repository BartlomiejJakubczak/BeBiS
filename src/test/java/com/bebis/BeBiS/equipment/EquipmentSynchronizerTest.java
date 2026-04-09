package com.bebis.BeBiS.equipment;

import com.bebis.BeBiS.equipment.jpa.EquipmentEntity;
import com.bebis.BeBiS.integration.blizzard.dto.EquipmentResponse;
import com.bebis.BeBiS.item.ItemService;
import com.bebis.BeBiS.item.jpa.EquippableItemEntity;
import com.bebis.BeBiS.item.jpa.ItemEntity;
import com.bebis.BeBiS.item.jpa.WeaponEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EquipmentSynchronizerTest {

    @Mock
    private ItemService itemService;

    private EquipmentSynchronizer synchronizer;

    @BeforeEach
    public void setUp() {
        synchronizer = new EquipmentSynchronizer(itemService);
    }

    @Test
    void shouldClearOldGearAndStayEmptyWhenResponseIsEmpty() {
        // given
        EquipmentEntity entity = new EquipmentEntity();
        entity.setItems(new HashMap<>());
        entity.getItems().put(Equipment.Slot.HEAD, new EquipmentEntity.EquippedItem());
        EquipmentResponse emptyResponse = new EquipmentResponse(List.of());

        // when
        synchronizer.synchronize(emptyResponse, entity);

        // then
        assertThat(entity.getItems()).isEmpty();
    }

    @Test
    void shouldMapSlotsCorrectlyAndApplyEnchants() {
        // given
        EquipmentEntity entity = new EquipmentEntity();
        entity.setItems(new HashMap<>());
        ItemEntity mockItem = new WeaponEntity();
        mockItem.setName("Thunderfury");

        // Mocking a Blizzard ItemDTO for the "Main Hand"
        EquipmentResponse.ItemDTO mainHandItem = mock(EquipmentResponse.ItemDTO.class);
        EquipmentResponse.ItemDTO.SlotDTO slot = mock(EquipmentResponse.ItemDTO.SlotDTO.class);
        EquipmentResponse.ItemDTO.ItemDTOReference itemRef = mock(EquipmentResponse.ItemDTO.ItemDTOReference.class);

        when(mainHandItem.slot()).thenReturn(slot);
        when(slot.type()).thenReturn("main_hand");
        when(mainHandItem.item()).thenReturn(itemRef);
        when(itemRef.id()).thenReturn(19019L);
        when(mainHandItem.getSuffixId()).thenReturn(0L);
        when(mainHandItem.getPlayerEnchantStrings()).thenReturn(List.of("Crusader"));

        when(itemService.getOrCreateEntity(19019L, 0L)).thenReturn(mockItem);

        EquipmentResponse response = new EquipmentResponse(List.of(mainHandItem));

        // when
        synchronizer.synchronize(response, entity);

        // then
        assertThat(entity.getItems()).containsKey(Equipment.Slot.MAIN_HAND);
        EquipmentEntity.EquippedItem equipped = entity.getItems().get(Equipment.Slot.MAIN_HAND);
        assertThat(equipped.getItem().getName()).isEqualTo("Thunderfury");
        assertThat(equipped.getPlayerEnchants()).containsExactly("Crusader");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldBatchSaveItemsFoundInResponse() {
        // given
        EquipmentEntity entity = new EquipmentEntity();
        entity.setItems(new HashMap<>());
        EquipmentResponse.ItemDTO item1 = createMockItemDTO("HEAD", 1L);
        EquipmentResponse.ItemDTO item2 = createMockItemDTO("NECK", 2L);
        EquipmentResponse response = new EquipmentResponse(List.of(item1, item2));

        EquippableItemEntity entity1 = new EquippableItemEntity();
        entity1.setName("test1");
        EquippableItemEntity entity2 = new EquippableItemEntity();
        entity1.setName("test2");

        when(itemService.getOrCreateEntity(anyLong(), anyLong()))
                .thenReturn(entity1, entity2); // return new entity on subsequent calls

        // when
        synchronizer.synchronize(response, entity);

        // then
        ArgumentCaptor<Set<ItemEntity>> captor = ArgumentCaptor.forClass(Set.class);
        verify(itemService).saveEntities(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
    }

    @Test
    void shouldHandleUnknownSlotsBySkippingThem() {
        // given
        EquipmentEntity entity = new EquipmentEntity();
        entity.setItems(new HashMap<>());
        EquipmentResponse.ItemDTO weirdItem = createMockItemDTO("POCKET_WATCH_SLOT", 99L);

        when(itemService.getOrCreateEntity(anyLong(), anyLong())).thenReturn(new EquippableItemEntity());
        EquipmentResponse response = new EquipmentResponse(List.of(weirdItem));

        // when
        synchronizer.synchronize(response, entity);

        // then
        assertThat(entity.getItems()).isEmpty(); // Should skip because Enum.valueOf fails
        verify(itemService).saveEntities(any());
    }

    private EquipmentResponse.ItemDTO createMockItemDTO(String slotType, long id) {
        EquipmentResponse.ItemDTO dto = mock(EquipmentResponse.ItemDTO.class);
        EquipmentResponse.ItemDTO.SlotDTO slot = mock(EquipmentResponse.ItemDTO.SlotDTO.class);
        EquipmentResponse.ItemDTO.ItemDTOReference itemRef = mock(EquipmentResponse.ItemDTO.ItemDTOReference.class);

        when(dto.slot()).thenReturn(slot);
        when(slot.type()).thenReturn(slotType);
        when(dto.item()).thenReturn(itemRef);
        when(itemRef.id()).thenReturn(id);

        return dto;
    }
}