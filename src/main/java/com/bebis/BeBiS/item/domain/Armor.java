package com.bebis.BeBiS.item.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public final class Armor extends Item {
    private ArmorType armorType;

    public Armor(ItemMetadata metadata, ArmorType armorType) {
        super(metadata);
        this.armorType = armorType;
    }

    public enum ArmorType {
        CLOTH,
        LEATHER,
        MAIL,
        PLATE,
        SHIELD,
        MISC
    }
}
