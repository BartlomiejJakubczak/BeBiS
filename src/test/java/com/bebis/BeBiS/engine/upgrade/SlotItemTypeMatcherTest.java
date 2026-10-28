package com.bebis.BeBiS.engine.upgrade;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Set;
import java.util.stream.Stream;

import static com.bebis.BeBiS.equipment.domain.Equipment.Slot;
import static com.bebis.BeBiS.equipment.domain.Equipment.Slot.*;
import static com.bebis.BeBiS.item.domain.Item.InventoryType;
import static org.assertj.core.api.Assertions.assertThat;

public class SlotItemTypeMatcherTest {

    private final SlotItemTypeMatcher slotItemTypeMatcher = new SlotItemTypeMatcher();

    @ParameterizedTest
    @MethodSource("slotToExpectedInventoryTypesProvider")
    void shouldMapSlotToItemTypes(Slot slot, Set<InventoryType> expectedInventoryType) {
        // when
        Set<InventoryType> matchedTypes = slotItemTypeMatcher.matchSlotsToItemTypes(Set.of(slot));

        // then
        assertThat(matchedTypes).isEqualTo(expectedInventoryType);
    }

    @Test
    void shouldMapMultipleSlotsToItemTypes() {
        // given
        Set<InventoryType> expectedInventoryTypes = Set.of(InventoryType.HEAD, InventoryType.RANGED, InventoryType.RANGEDRIGHT, InventoryType.THROWN);

        // when
        Set<InventoryType> matchedTypes = slotItemTypeMatcher.matchSlotsToItemTypes(Set.of(HEAD, RANGED));

        // then
        assertThat(matchedTypes).isEqualTo(expectedInventoryTypes);
    }

    private static Stream<Arguments> slotToExpectedInventoryTypesProvider() {
        return Stream.of(
                Arguments.of(HEAD, Set.of(InventoryType.HEAD)),
                Arguments.of(NECK, Set.of(InventoryType.NECK)),
                Arguments.of(SHOULDER, Set.of(InventoryType.SHOULDER)),
                Arguments.of(BACK, Set.of(InventoryType.CLOAK)),
                Arguments.of(CHEST, Set.of(InventoryType.CHEST)),
                Arguments.of(WRIST, Set.of(InventoryType.WRIST)),
                Arguments.of(HANDS, Set.of(InventoryType.HANDS)),
                Arguments.of(WAIST, Set.of(InventoryType.WAIST)),
                Arguments.of(LEGS, Set.of(InventoryType.LEGS)),
                Arguments.of(FEET, Set.of(InventoryType.FEET)),
                Arguments.of(FINGER_1, Set.of(InventoryType.FINGER)),
                Arguments.of(FINGER_2, Set.of(InventoryType.FINGER)),
                Arguments.of(TRINKET_1, Set.of(InventoryType.TRINKET)),
                Arguments.of(TRINKET_2, Set.of(InventoryType.TRINKET)),
                Arguments.of(MAIN_HAND, Set.of(InventoryType.WEAPON, InventoryType.WEAPONMAINHAND, InventoryType.TWO_HAND)),
                Arguments.of(OFF_HAND, Set.of(InventoryType.WEAPON, InventoryType.WEAPONOFFHAND, InventoryType.HOLDABLE, InventoryType.SHIELD)),
                Arguments.of(RANGED, Set.of(InventoryType.RANGED, InventoryType.RANGEDRIGHT, InventoryType.THROWN))
        );
    }
}
