package com.bebis.BeBiS.item.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public final class EquippableItem extends Item {
    public EquippableItem(ItemMetadata metadata) {
        super(metadata);
    }
}
