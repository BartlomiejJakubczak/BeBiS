package com.bebis.BeBiS.item.jpa;

import com.bebis.BeBiS.item.Weapon;
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
@DiscriminatorValue("WEAPON")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class WeaponEntity extends ItemEntity {
    private Double speed;

    @Column(name = "min_damage")
    private Integer minDamage;

    @Column(name = "max_damage")
    private Integer maxDamage;

    private Double dps;

    @Column(name = "weapon_type")
    @Enumerated(value = EnumType.STRING)
    private Weapon.WeaponType weaponType;
}
