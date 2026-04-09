package com.bebis.BeBiS.equipment;

import com.bebis.BeBiS.item.Item;

import java.util.List;

public record EquippedItem(
        Item item,
        List<String> playerEnchs
) {
}
