package com.bebis.BeBiS.equipment;

import com.bebis.BeBiS.equipment.jpa.EquipmentEntity;
import com.bebis.BeBiS.integration.blizzard.dto.EquipmentResponse;
import com.bebis.BeBiS.item.ItemService;
import com.bebis.BeBiS.item.jpa.ItemEntity;
import com.bebis.BeBiS.item.jpa.WeaponEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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

        EquipmentResponse emptyResponse = mock(EquipmentResponse.class);
        when(emptyResponse.equipment()).thenReturn(List.of());

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

        ItemEntity mockItem = new WeaponEntity(); // Real entity for reference check
        EquipmentResponse.ItemDTO itemDto = mock(EquipmentResponse.ItemDTO.class);
        EquipmentResponse.ItemDTO.SlotDTO slotDto = mock(EquipmentResponse.ItemDTO.SlotDTO.class);
        EquipmentResponse.ItemDTO.ItemDTOReference itemRef = mock(EquipmentResponse.ItemDTO.ItemDTOReference.class);

        // Stubbing the DTO "Pipe"
        when(itemDto.slot()).thenReturn(slotDto);
        when(slotDto.type()).thenReturn("main_hand");
        when(itemDto.item()).thenReturn(itemRef);
        when(itemRef.id()).thenReturn(19019L);
        when(itemDto.getSuffixId()).thenReturn(0L);
        when(itemDto.getPlayerEnchantStrings()).thenReturn(List.of("Crusader"));

        when(itemService.getOrCreateEntity(19019L, 0L)).thenReturn(mockItem);

        EquipmentResponse response = mock(EquipmentResponse.class);
        when(response.equipment()).thenReturn(List.of(itemDto));

        // when
        synchronizer.synchronize(response, entity);

        // then
        assertThat(entity.getItems()).containsKey(Equipment.Slot.MAIN_HAND);
        EquipmentEntity.EquippedItem equipped = entity.getItems().get(Equipment.Slot.MAIN_HAND);

        assertThat(equipped.getItem()).isSameAs(mockItem);
        assertThat(equipped.getPlayerEnchants()).containsExactly("Crusader");
        assertThat(equipped.getFullName()).isNull(); // No suffix, so no full name override
    }

    @Test
    void shouldApplyFullNameToEquippedItemWhenSuffixIsPresent() {
        // given
        EquipmentEntity entity = new EquipmentEntity();
        entity.setItems(new HashMap<>());

        long itemId = 12345L;
        long suffixId = 37L;
        String fullName = "Ironforge Breastplate of the Tiger";

        ItemEntity baseItem = new WeaponEntity();
        EquipmentResponse.ItemDTO itemDto = mock(EquipmentResponse.ItemDTO.class);
        EquipmentResponse.ItemDTO.SlotDTO slotDto = mock(EquipmentResponse.ItemDTO.SlotDTO.class);
        EquipmentResponse.ItemDTO.ItemDTOReference itemRef = mock(EquipmentResponse.ItemDTO.ItemDTOReference.class);

        when(itemDto.slot()).thenReturn(slotDto);
        when(slotDto.type()).thenReturn("chest");
        when(itemDto.item()).thenReturn(itemRef);
        when(itemRef.id()).thenReturn(itemId);

        // Trigger the Full Name logic
        when(itemDto.getSuffixId()).thenReturn(suffixId);
        when(itemDto.name()).thenReturn(fullName);

        when(itemService.getOrCreateEntity(itemId, suffixId)).thenReturn(baseItem);

        EquipmentResponse response = mock(EquipmentResponse.class);
        when(response.equipment()).thenReturn(List.of(itemDto));

        // when
        synchronizer.synchronize(response, entity);

        // then
        EquipmentEntity.EquippedItem equipped = entity.getItems().get(Equipment.Slot.CHEST);
        assertThat(equipped.getFullName()).isEqualTo(fullName);
        // Ensure we haven't touched the base item's generic name
        assertThat(baseItem.getName()).isNotEqualTo(fullName);
    }

    @Test
    void shouldHandleUnknownSlotsBySkippingThem() {
        // given
        EquipmentEntity entity = new EquipmentEntity();
        entity.setItems(new HashMap<>());

        EquipmentResponse.ItemDTO weirdItem = mock(EquipmentResponse.ItemDTO.class);
        EquipmentResponse.ItemDTO.SlotDTO slotDto = mock(EquipmentResponse.ItemDTO.SlotDTO.class);

        when(weirdItem.slot()).thenReturn(slotDto);
        when(slotDto.type()).thenReturn("POCKET_WATCH_SLOT");

        EquipmentResponse response = mock(EquipmentResponse.class);
        when(response.equipment()).thenReturn(List.of(weirdItem));

        // when
        synchronizer.synchronize(response, entity);

        // then
        assertThat(entity.getItems()).isEmpty();
        verifyNoInteractions(itemService); // Optimization: if slot is invalid, don't even fetch the item
    }
}