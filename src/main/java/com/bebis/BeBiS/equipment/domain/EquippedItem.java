package com.bebis.BeBiS.equipment.domain;

import com.bebis.BeBiS.item.domain.Item;

import java.util.List;

public record EquippedItem(
        Item item,
        List<String> playerEnchs
) {
}
