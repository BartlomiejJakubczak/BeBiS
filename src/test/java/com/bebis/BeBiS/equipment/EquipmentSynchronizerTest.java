package com.bebis.BeBiS.equipment;

import com.bebis.BeBiS.equipment.jpa.EquipmentEntity;
import com.bebis.BeBiS.integration.blizzard.dto.EquipmentResponse;
import com.bebis.BeBiS.integration.blizzard.dto.ItemResponse;
import com.bebis.BeBiS.item.ItemService;
import com.bebis.BeBiS.item.ItemTestData;
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
        ItemResponse tf = ItemTestData.thunderfuryResponse();
        EquipmentResponse.ItemDTO itemDTO = EquipmentTestData.fromItemResponseNoSuffix(
                tf, "main_hand", List.of(EquipmentTestData.enchant(2137L, "Crusader")));

        when(itemService.getOrCreateEntity(tf.id(), itemDTO)).thenReturn(mockItem);

        EquipmentResponse response = mock(EquipmentResponse.class);
        when(response.equipment()).thenReturn(List.of(itemDTO));

        // when
        synchronizer.synchronize(response, entity);

        // then
        verify(itemService).getOrCreateEntity(eq(tf.id()),
                argThat(dto -> dto.getPlayerEnchantStrings().contains("Crusader") && dto.getSuffixId() == 0L));

        assertThat(entity.getItems()).containsKey(Equipment.Slot.MAIN_HAND);
        EquipmentEntity.EquippedItem equipped = entity.getItems().get(Equipment.Slot.MAIN_HAND);
        
        assertThat(equipped.getItem()).isSameAs(mockItem);
        assertThat(equipped.getPlayerEnchants()).containsExactly("Crusader");
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