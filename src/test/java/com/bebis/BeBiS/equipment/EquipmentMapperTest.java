package com.bebis.BeBiS.equipment;

import com.bebis.BeBiS.equipment.domain.Equipment;
import com.bebis.BeBiS.equipment.jpa.EquipmentEntity;
import com.bebis.BeBiS.integration.blizzard.dto.EquipmentResponse;
import com.bebis.BeBiS.integration.blizzard.dto.ItemResponse;
import com.bebis.BeBiS.item.EquippableItemBuilder;
import com.bebis.BeBiS.item.ItemResponseBuilder;
import com.bebis.BeBiS.item.domain.Item;
import com.bebis.BeBiS.item.jpa.EquippableItemEntity;
import com.bebis.BeBiS.item.jpa.ItemEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class EquipmentMapperTest {

    private final EquipmentMapper mapper = new EquipmentMapper();

    @Test
    void shouldPutTheSameAssembledItemIntoBothSlots_WhenTwoIdenticalRingsAreEquipped() {
        // given
        ItemEntity ringEntity = new EquippableItemEntity();
        ringEntity.setPk(new ItemEntity.CompositeKey(12022L, 37L)); // Gold Ring of the Monkey, not unique-equipped

        EquipmentEntity equipmentEntity = new EquipmentEntity();
        equipmentEntity.getItems().put(Equipment.Slot.FINGER_1, equipped(ringEntity, List.of()));
        equipmentEntity.getItems().put(Equipment.Slot.FINGER_2, equipped(ringEntity, List.of("+7 Agility")));

        Item ring = EquippableItemBuilder.newRing().withBaseId(12022L).withSuffixId(37L).build();
        Map<Item.ItemKey, Item> itemsByKey = Map.of(ring.getMetadata().key(), ring);

        // when
        Equipment result = mapper.mapToDomain(equipmentEntity, itemsByKey);

        // then
        assertThat(result.getItem(Equipment.Slot.FINGER_1)).hasValueSatisfying(equipped -> assertThat(equipped.item()).isSameAs(ring));
        assertThat(result.getItem(Equipment.Slot.FINGER_2)).hasValueSatisfying(equipped -> {
            assertThat(equipped.item()).isSameAs(ring);
            assertThat(equipped.playerEnchs()).containsExactly("+7 Agility");
        });
    }

    private static EquipmentEntity.EquippedItem equipped(ItemEntity item, List<String> enchants) {
        EquipmentEntity.EquippedItem equippedItem = new EquipmentEntity.EquippedItem();
        equippedItem.setItem(item);
        equippedItem.setPlayerEnchants(enchants);
        return equippedItem;
    }

    @ParameterizedTest
    @ValueSource(strings = {"BELT", ""})
    void shouldReturnEmptyOptionalWhenSlotIs(String slot) {
        // given
        ItemResponse response = ItemResponseBuilder.newWeaponInstance().build();
        EquipmentResponse.ItemDTO dto = EquipmentResponseBuilder.newInstance(response)
                .withSlot(slot)
                .build();

        // when
        Optional<Equipment.Slot> result = mapper.mapSlot(dto);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldMapSlotCorrectly() {
        // given
        String correctSlot = "MAIN_HAND";

        ItemResponse response = ItemResponseBuilder.newWeaponInstance().build();
        EquipmentResponse.ItemDTO dto = EquipmentResponseBuilder.newInstance(response)
                .withSlot(correctSlot)
                .build();

        // when
        Optional<Equipment.Slot> result = mapper.mapSlot(dto);

        // then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(Equipment.Slot.MAIN_HAND);
    }

    @Test
    void shouldReturnEmptyOptionalWhenEnchantmentsAreNull() {
        // given
        ItemResponse response = ItemResponseBuilder.newWeaponInstance().build();
        EquipmentResponse.ItemDTO dto = EquipmentResponseBuilder.newInstance(response)
                .withSlot("MAIN_HAND")
                .withEnchantments(null)
                .build();

        // when
        List<String> result = mapper.mapPlayerEnchants(dto, 0L);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldMapEnchantmentsOfSuffixedItemsCorrectly() {
        // given
        long suffixId = 37L;
        List<String> expectedEnchantments = List.of("Crusader"); // suffixId is counted by Blizz as an enchantment,
        // so it has to be omitted by the mapper explicitly

        ItemResponse response = ItemResponseBuilder.newSuffixableWeaponInstance().build();

        EquipmentResponse.ItemDTO dto = EquipmentResponseBuilder.newInstance(response)
                .withSuffix(new EquipmentResponseBuilder.Suffix(suffixId, "of the Bear"))
                .withEnchantments(Map.of(69L, expectedEnchantments.getFirst()))
                .withSlot("MAIN_HAND")
                .build();

        // when
        List<String> result = mapper.mapPlayerEnchants(dto, suffixId);

        // then
        assertThat(result).containsExactlyInAnyOrder(expectedEnchantments.getFirst());
    }
}
