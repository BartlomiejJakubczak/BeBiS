package com.bebis.BeBiS.item.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("WEAPON")
public class WeaponEntity extends ItemEntity {

    private double speed;

    @Column(name = "min_damage")
    private int minDamage;

    @Column(name = "max_damage")
    private int maxDamage;
    
    private double dps;
}
