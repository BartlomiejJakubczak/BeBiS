package com.bebis.BeBiS.engine.upgrade;

import com.bebis.BeBiS.engine.upgrade.domain.HandSlotCombinations;
import com.bebis.BeBiS.engine.upgrade.domain.HandSlotTypes;
import com.bebis.BeBiS.item.domain.Item;
import com.bebis.BeBiS.profile.domain.WowCharacter;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.bebis.BeBiS.item.domain.Item.InventoryType.*;
import static com.bebis.BeBiS.profile.domain.WowCharacter.WowClass.*;

@Component
final class ClassHandSlotRegistry {

    private static final Map<WowCharacter.WowClass, HandSlotCombinations> registry = new EnumMap<>(WowCharacter.WowClass.class);

    static {
        registry.put(WARRIOR, HandSlotCombinations.from(new HandSlotTypes(List.of(TWO_HAND), List.of()), new HandSlotTypes(List.of(WEAPON, WEAPONMAINHAND), List.of(WEAPON, WEAPONOFFHAND, SHIELD))));
        registry.put(ROGUE, HandSlotCombinations.from(new HandSlotTypes(List.of(WEAPON, WEAPONMAINHAND), List.of(WEAPON, WEAPONOFFHAND))));
        registry.put(PALADIN, HandSlotCombinations.from(new HandSlotTypes(List.of(TWO_HAND), List.of()), new HandSlotTypes(List.of(WEAPON, WEAPONMAINHAND), List.of(SHIELD))));
        registry.put(SHAMAN, HandSlotCombinations.from(new HandSlotTypes(List.of(TWO_HAND), List.of()), new HandSlotTypes(List.of(WEAPON, WEAPONMAINHAND), List.of(HOLDABLE, SHIELD))));
        registry.put(PRIEST, HandSlotCombinations.from(new HandSlotTypes(List.of(TWO_HAND), List.of()), new HandSlotTypes(List.of(WEAPON, WEAPONMAINHAND), List.of(HOLDABLE))));
        registry.put(MAGE, HandSlotCombinations.from(new HandSlotTypes(List.of(TWO_HAND), List.of()), new HandSlotTypes(List.of(WEAPON, WEAPONMAINHAND), List.of(HOLDABLE))));
        registry.put(WARLOCK, HandSlotCombinations.from(new HandSlotTypes(List.of(TWO_HAND), List.of()), new HandSlotTypes(List.of(WEAPON, WEAPONMAINHAND), List.of(HOLDABLE))));
        registry.put(HUNTER, HandSlotCombinations.from(new HandSlotTypes(List.of(TWO_HAND), List.of()), new HandSlotTypes(List.of(WEAPON, WEAPONMAINHAND), List.of(WEAPON, WEAPONOFFHAND))));
        registry.put(DRUID, HandSlotCombinations.from(new HandSlotTypes(List.of(TWO_HAND), List.of()), new HandSlotTypes(List.of(WEAPON, WEAPONMAINHAND), List.of(HOLDABLE))));
    }

    private static final Map<WowCharacter.WowClass, Integer> dualWieldingReqLevels = Map.of(WARRIOR, 10, ROGUE, 10, HUNTER, 20);

    private static final Set<Item.InventoryType> dualWieldingTypes = Set.of(WEAPON, WEAPONOFFHAND);

    HandSlotCombinations getHandSlotCombinationFor(WowCharacter.WowClass wowClass, int level) {
        HandSlotCombinations combo = registry.getOrDefault(wowClass, HandSlotCombinations.from());
        int dualWieldingLevel = dualWieldingReqLevels.getOrDefault(wowClass, 0);
        if (level >= dualWieldingLevel) {
            return combo;
        }
        return new HandSlotCombinations(combo.validHandSlotTypes().stream()
                .map((types) -> isADualWieldingType(types) ? getNonDualWieldingTypes(types) : types)
                .toList());
    }

    private boolean isADualWieldingType(HandSlotTypes types) {
        return types.offHand().stream().anyMatch(dualWieldingTypes::contains);
    }

    private HandSlotTypes getNonDualWieldingTypes(HandSlotTypes types) {
        return new HandSlotTypes(types.mainHand(), types.offHand().stream().filter((type) -> !dualWieldingTypes.contains(type)).toList());
    }
}
