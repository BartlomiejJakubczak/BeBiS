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
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class EquipmentMapperTest {

    private final EquipmentMapper mapper = new EquipmentMapper(new ItemMapper());

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
