package com.bebis.BeBiS.engine.upgrade;

import com.bebis.BeBiS.engine.upgrade.domain.HandSlotCombinations;
import com.bebis.BeBiS.engine.upgrade.domain.HandSlotTypes;
import com.bebis.BeBiS.item.domain.Item;
import com.bebis.BeBiS.profile.domain.WowCharacter;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Set;
import java.util.stream.Stream;

import static com.bebis.BeBiS.item.domain.Item.InventoryType.WEAPON;
import static com.bebis.BeBiS.item.domain.Item.InventoryType.WEAPONOFFHAND;
import static com.bebis.BeBiS.profile.domain.WowCharacter.WowClass.*;
import static org.assertj.core.api.Assertions.assertThat;

public class ClassHandSlotRegistryTest {

    private static final Set<Item.InventoryType> dualWieldingTypes = Set.of(WEAPON, WEAPONOFFHAND);

    private final ClassHandSlotRegistry registry = new ClassHandSlotRegistry();

    @ParameterizedTest
    @MethodSource("dualWieldingClassesAndTheirDwLevelReq")
    void shouldReturnDualWieldingHandSlotCombosForEligibleClassesAtEligibleLevel(WowCharacter.WowClass dwClass, int dwLevelReq) {
        // given / when
        HandSlotCombinations result = registry.getHandSlotCombinationFor(dwClass, dwLevelReq);
        // then
        assertThat(result.validHandSlotTypes()).extracting(HandSlotTypes::offHand)
                .anyMatch((offHands) -> offHands.containsAll(dualWieldingTypes));
    }

    private static Stream<Arguments> dualWieldingClassesAndTheirDwLevelReq() {
        return Stream.of(
                Arguments.of(WARRIOR, 10),
                Arguments.of(ROGUE, 10),
                Arguments.of(HUNTER, 20)
        );
    }

    @ParameterizedTest
    @MethodSource("dualWieldingClassesAndLevelBelowDwReq")
    void shouldNotReturnDualWieldingHandSlotCombosForEligibleClassesAtLowLevel(WowCharacter.WowClass dwClass, int level) {
        // given / when
        HandSlotCombinations result = registry.getHandSlotCombinationFor(dwClass, level);
        // then
        assertThat(result.validHandSlotTypes()).extracting(HandSlotTypes::offHand)
                .noneMatch((offHands) -> offHands.containsAll(dualWieldingTypes));
    }

    private static Stream<Arguments> dualWieldingClassesAndLevelBelowDwReq() {
        return Stream.of(
                Arguments.of(WARRIOR, 9),
                Arguments.of(ROGUE, 9),
                Arguments.of(HUNTER, 19)
        );
    }

    @ParameterizedTest
    @EnumSource(value = WowCharacter.WowClass.class, names = {"PALADIN", "SHAMAN", "PRIEST", "MAGE", "WARLOCK", "DRUID"})
    void shouldNotReturnDualWieldingHandSlotCombosForNonEligibleClasses(WowCharacter.WowClass nonEligibleClass) {
        // when
        HandSlotCombinations result = registry.getHandSlotCombinationFor(nonEligibleClass, 60);
        // then
        assertThat(result.validHandSlotTypes()).extracting(HandSlotTypes::offHand)
                .noneMatch((offHands) -> offHands.containsAll(dualWieldingTypes));
    }

}
