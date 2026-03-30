package com.bebis.BeBiS.equipment;

import com.bebis.BeBiS.item.Item;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public final class Equipment {
    private Map<Slot, Item> equipment;

    public Equipment() {
        this.equipment = new HashMap<>();
    }

    public enum Slot {
        HEAD,
        NECK,
        SHOULDER,
        BACK,
        CHEST,
        SHIRT,
        TABARD,
        WRIST,
        HANDS,
        WAIST,
        LEGS,
        FEET,
        FINGER_1,
        FINGER_2,
        TRINKET_1,
        TRINKET_2,
        MAIN_HAND,
        OFF_HAND,
        RANGED
    }

    /*
        A naive transposition. More complex logic will be done in some kind of bis finder service, like for example
        if main_hand is a two hand, then off hand is off limits and so on.
     */
    public void putItem(Slot slot, Item item) {
        equipment.put(slot, item);
    }
}
