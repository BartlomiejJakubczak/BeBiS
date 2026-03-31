package com.bebis.BeBiS.item.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("ARMOR")
public class ArmorEntity extends ItemEntity {

    @Column(name = "armor_value")
    private int armorValue;
}
