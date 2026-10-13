package com.bebis.BeBiS.engine.upgrade.domain;

import java.util.List;

public record HandSlotCombinations(List<HandSlotTypes> validHandSlotTypes) {

    public static HandSlotCombinations from(HandSlotTypes... types) {
        return new HandSlotCombinations(List.of(types));
    }
}
