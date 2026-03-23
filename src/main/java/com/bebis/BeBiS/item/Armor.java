package com.bebis.BeBiS.item;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public final class Armor extends Item {
    private int armorValue;
    private ArmorType armorType;

    public Armor(ItemMetadata metadata, int armorValue, ArmorType armorType) {
        super(metadata);
        this.armorValue = armorValue;
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
