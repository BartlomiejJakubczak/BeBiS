package com.bebis.BeBiS.engine.upgrade.domain;

import com.bebis.BeBiS.item.domain.Item;

import java.util.List;

public record HandSlotTypes(List<Item.InventoryType> mainHand, List<Item.InventoryType> offHand) {
}
