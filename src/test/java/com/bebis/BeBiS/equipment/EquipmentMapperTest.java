package com.bebis.BeBiS.equipment;

import com.bebis.BeBiS.equipment.domain.Equipment;
import com.bebis.BeBiS.integration.blizzard.dto.EquipmentResponse;
import com.bebis.BeBiS.integration.blizzard.dto.ItemResponse;
import com.bebis.BeBiS.item.ItemMapper;
import com.bebis.BeBiS.item.ItemResponseBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class EquipmentMapperTest {

    private final EquipmentMapper mapper = new EquipmentMapper(new ItemMapper());

    @ParameterizedTest
    @ValueSource(strings = {"BELT", ""})
    void shouldReturnEmptyOptionalWhenSlotIs(String slot) {
        // given
        ItemResponse response = ItemResponseBuilder.newWeaponInstance().build();
        EquipmentResponse.ItemDTO dto = EquipmentTestData.fromItemResponseNoSuffix(response, slot, List.of());

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
        EquipmentResponse.ItemDTO dto = EquipmentTestData.fromItemResponseNoSuffix(response, correctSlot, List.of());

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
        EquipmentResponse.ItemDTO dto = EquipmentTestData.fromItemResponseNoSuffix(response, "MAIN_HAND", null);

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

        ItemResponse response = ItemResponseBuilder.newEquippableInstance().build();
        EquipmentResponse.ItemDTO dto = EquipmentTestData.fromItemResponseSuffixed(response, "FINGER_1", "RARE",
                "of The Bear", suffixId, 60, List.of(), List.of(EquipmentTestData.enchant(69L, expectedEnchantments.getFirst())));

        // when
        List<String> result = mapper.mapPlayerEnchants(dto, suffixId);

        // then
        assertThat(result).containsExactlyInAnyOrder(expectedEnchantments.getFirst());
    }
}
