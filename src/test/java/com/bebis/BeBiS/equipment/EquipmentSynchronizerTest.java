package com.bebis.BeBiS.equipment;

import com.bebis.BeBiS.equipment.domain.Equipment;
import com.bebis.BeBiS.equipment.jpa.EquipmentEntity;
import com.bebis.BeBiS.integration.blizzard.dto.EquipmentResponse;
import com.bebis.BeBiS.integration.blizzard.dto.ItemResponse;
import com.bebis.BeBiS.item.ItemMapper;
import com.bebis.BeBiS.item.ItemResponseBuilder;
import com.bebis.BeBiS.item.ItemService;
import com.bebis.BeBiS.item.jpa.EquippableItemEntity;
import com.bebis.BeBiS.item.jpa.ItemEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EquipmentSynchronizerTest {

    @Mock
    private ItemService itemService;

    private EquipmentSynchronizer synchronizer;

    @BeforeEach
    public void setUp() {
        synchronizer = new EquipmentSynchronizer(itemService, new EquipmentMapper(new ItemMapper()));
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
    void shouldClearOldGearAndStayEmptyWhenResolvedItemsFailedForAllItemsFromResponse() {
        // given
        EquipmentEntity entity = new EquipmentEntity();
        entity.setItems(new HashMap<>());
        entity.getItems().put(Equipment.Slot.HEAD, new EquipmentEntity.EquippedItem());

        ItemResponse fingerResponse = ItemResponseBuilder.newEquippableInstance().build();
        EquipmentResponse.ItemDTO fingerDTO = EquipmentResponseBuilder.newInstance(fingerResponse)
                .withSlot("FINGER_1")
                .build();

        ItemResponse chestResponse = ItemResponseBuilder.newArmorInstance()
                .withId(2L)
                .withName("Breastplate")
                .build();
        EquipmentResponse.ItemDTO chestDTO = EquipmentResponseBuilder.newInstance(chestResponse)
                .withSlot("CHEST")
                .build();

        ItemResponse legsResponse = ItemResponseBuilder.newArmorInstance()
                .withId(3L)
                .withName("Leggings")
                .build();
        EquipmentResponse.ItemDTO legsDTO = EquipmentResponseBuilder.newInstance(legsResponse)
                .withSlot("LEGS")
                .build();

        EquipmentResponse response = mock(EquipmentResponse.class);
        when(response.equipment()).thenReturn(List.of(fingerDTO, chestDTO, legsDTO));

        when(itemService.resolveItems(response.equipment())).thenReturn(Map.of()); // resolved items failed for the whole response

        // when
        synchronizer.synchronize(response, entity);

        // then
        assertThat(entity.getItems()).isEmpty();
    }

    @Test
    void shouldNotAssignAnInvalidItemToTheSlot() {
        // given
        ItemResponse fingerResponse = ItemResponseBuilder.newEquippableInstance().build();

        EquipmentResponse.ItemDTO fingerDTO = EquipmentResponseBuilder.newInstance(fingerResponse)
                .withSlot("FINGER_1")
                .build();

        ItemResponse chestResponse = ItemResponseBuilder.newArmorInstance()
                .withId(2L)
                .withName(null) // null name
                .build();

        EquipmentResponse.ItemDTO chestDTO = EquipmentResponseBuilder.newInstance(chestResponse)
                .withSlot("CHEST")
                .build();

        EquipmentResponse response = mock(EquipmentResponse.class);
        when(response.equipment()).thenReturn(List.of(fingerDTO, chestDTO));

        // chest piece was faulty, result of resolving doesnt matter
        EquippableItemEntity resolvedEntity = mock(EquippableItemEntity.class);
        when(resolvedEntity.getPk()).thenReturn(new ItemEntity.CompositeKey(fingerResponse.id(), 0L));

        when(itemService.resolveItems(response.equipment())).thenReturn(Map.of(fingerDTO, resolvedEntity));

        EquipmentEntity entity = new EquipmentEntity();

        // when
        synchronizer.synchronize(response, entity);

        // then
        assertThat(entity.getItems().get(Equipment.Slot.FINGER_1)).isNotNull();
        assertThat(entity.getItems().get(Equipment.Slot.CHEST)).isNull();
    }
}