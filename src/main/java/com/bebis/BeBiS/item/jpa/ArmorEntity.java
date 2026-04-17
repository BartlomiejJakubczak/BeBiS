package com.bebis.BeBiS.item.jpa;

import com.bebis.BeBiS.item.Armor;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@DiscriminatorValue("ARMOR")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class ArmorEntity extends ItemEntity {

    @Column(name = "armor_type")
    @Enumerated(value = EnumType.STRING)
    private Armor.ArmorType armorType;
}
