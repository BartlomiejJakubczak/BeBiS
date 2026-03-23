package com.bebis.BeBiS.item;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public final class Weapon extends Item {
    private double speed;
    private int minDamage;
    private int maxDamage;
    private WeaponType weaponType;

    public Weapon(ItemMetadata itemMetadata, double speed, int minDamage, int maxDamage, WeaponType weaponType) {
        super(itemMetadata);
        this.speed = speed;
        this.minDamage = minDamage;
        this.maxDamage = maxDamage;
        this.weaponType = weaponType;
    }

    public enum WeaponType {
        SWORD, AXE, MACE, DAGGER, POLEARM, STAFF, BOW, CROSSBOW, GUN, WAND, UNARMED
    }
}
