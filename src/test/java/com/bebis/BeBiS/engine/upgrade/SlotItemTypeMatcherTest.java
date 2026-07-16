package com.bebis.BeBiS.engine.upgrade;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Set;
import java.util.stream.Stream;

import static com.bebis.BeBiS.equipment.domain.Equipment.Slot;
import static com.bebis.BeBiS.item.domain.Item.InventoryType;
import static org.assertj.core.api.Assertions.assertThat;

public class SlotItemTypeMatcherTest {

    private final SlotItemTypeMatcher slotItemTypeMatcher = new SlotItemTypeMatcher();

    @ParameterizedTest
    @MethodSource("slotToExpectedInventoryTypesProvider")
    void shouldMapSlotToItemTypes(Slot slot, Set<InventoryType> expectedInventoryType) {

        // when
        Set<InventoryType> matchedTypes = slotItemTypeMatcher.matchSlotToItemTypes(slot);

        // then
        assertThat(matchedTypes).isEqualTo(expectedInventoryType);
    }

    private static Stream<Arguments> slotToExpectedInventoryTypesProvider() {
        return Stream.of(
                Arguments.of(Slot.HEAD, Set.of(InventoryType.HEAD)),
                Arguments.of(Slot.NECK, Set.of(InventoryType.NECK)),
                Arguments.of(Slot.SHOULDER, Set.of(InventoryType.SHOULDER)),
                Arguments.of(Slot.BACK, Set.of(InventoryType.CLOAK)),
                Arguments.of(Slot.CHEST, Set.of(InventoryType.CHEST)),
                Arguments.of(Slot.WRIST, Set.of(InventoryType.WRIST)),
                Arguments.of(Slot.HANDS, Set.of(InventoryType.HANDS)),
                Arguments.of(Slot.WAIST, Set.of(InventoryType.WAIST)),
                Arguments.of(Slot.LEGS, Set.of(InventoryType.LEGS)),
                Arguments.of(Slot.FEET, Set.of(InventoryType.FEET)),
                Arguments.of(Slot.FINGER_1, Set.of(InventoryType.FINGER)),
                Arguments.of(Slot.FINGER_2, Set.of(InventoryType.FINGER)),
                Arguments.of(Slot.TRINKET_1, Set.of(InventoryType.TRINKET)),
                Arguments.of(Slot.TRINKET_2, Set.of(InventoryType.TRINKET)),
                Arguments.of(Slot.MAIN_HAND, Set.of(InventoryType.WEAPON, InventoryType.WEAPONMAINHAND, InventoryType.TWO_HAND)),
                Arguments.of(Slot.OFF_HAND, Set.of(InventoryType.WEAPON, InventoryType.WEAPONOFFHAND, InventoryType.HOLDABLE, InventoryType.SHIELD)),
                Arguments.of(Slot.RANGED, Set.of(InventoryType.RANGED, InventoryType.RANGEDRIGHT, InventoryType.THROWN))
        );
    }
}
