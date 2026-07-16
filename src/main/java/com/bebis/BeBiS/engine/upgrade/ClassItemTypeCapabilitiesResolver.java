package com.bebis.BeBiS.engine.upgrade;

import com.bebis.BeBiS.item.domain.Armor;
import com.bebis.BeBiS.item.domain.Weapon;
import com.bebis.BeBiS.profile.domain.WowCharacter;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

import static com.bebis.BeBiS.item.domain.Armor.ArmorType;
import static com.bebis.BeBiS.item.domain.Weapon.WeaponType;

@Component
class ClassItemTypeCapabilitiesResolver {

    private static final int ARMOR_TRANSITION_LEVEL = 40;

    ClassItemTypeCapabilities getCapabilitiesFor(WowCharacter.WowClass wowClass, int level) {
        return switch (wowClass) {
            case WARRIOR -> new ClassItemTypeCapabilities(
                    getWarriorArmorForLevel(level),
                    getPolearmGatedWeapons(level, WeaponType.AXE, WeaponType.SWORD, WeaponType.MACE,
                            WeaponType.STAFF, WeaponType.DAGGER, WeaponType.BOW, WeaponType.CROSSBOW,
                            WeaponType.GUN, WeaponType.UNARMED)
            );

            case PALADIN -> new ClassItemTypeCapabilities(
                    getPaladinArmorForLevel(level),
                    getPolearmGatedWeapons(level, WeaponType.MACE, WeaponType.SWORD, WeaponType.AXE, WeaponType.UNARMED)
            );

            case HUNTER -> new ClassItemTypeCapabilities(
                    getHunterArmorForLevel(level),
                    getPolearmGatedWeapons(level, WeaponType.STAFF, WeaponType.AXE, WeaponType.SWORD,
                            WeaponType.DAGGER, WeaponType.BOW, WeaponType.CROSSBOW, WeaponType.GUN,
                            WeaponType.UNARMED)
            );

            case SHAMAN -> new ClassItemTypeCapabilities(
                    getShamanArmorForLevel(level),
                    Set.of(WeaponType.MACE, WeaponType.AXE, WeaponType.STAFF, WeaponType.DAGGER,
                            WeaponType.UNARMED)
            );

            case ROGUE -> new ClassItemTypeCapabilities(
                    Set.of(ArmorType.CLOTH, ArmorType.LEATHER),
                    Set.of(WeaponType.SWORD, WeaponType.DAGGER, WeaponType.MACE, WeaponType.BOW,
                            WeaponType.CROSSBOW, WeaponType.GUN, WeaponType.UNARMED)
            );

            case DRUID -> new ClassItemTypeCapabilities(
                    Set.of(ArmorType.CLOTH, ArmorType.LEATHER),
                    Set.of(WeaponType.STAFF, WeaponType.MACE, WeaponType.DAGGER, WeaponType.UNARMED)
            );

            case MAGE -> new ClassItemTypeCapabilities(
                    Set.of(ArmorType.CLOTH),
                    Set.of(WeaponType.WAND, WeaponType.STAFF, WeaponType.DAGGER, WeaponType.SWORD, WeaponType.UNARMED)
            );

            case WARLOCK -> new ClassItemTypeCapabilities(
                    Set.of(ArmorType.CLOTH),
                    Set.of(WeaponType.WAND, WeaponType.STAFF, WeaponType.DAGGER, WeaponType.SWORD, WeaponType.UNARMED)
            );

            case PRIEST -> new ClassItemTypeCapabilities(
                    Set.of(ArmorType.CLOTH),
                    Set.of(WeaponType.WAND, WeaponType.STAFF, WeaponType.DAGGER, WeaponType.MACE, WeaponType.UNARMED)
            );
        };
    }

    record ClassItemTypeCapabilities(
            Set<Armor.ArmorType> eligibleArmor,
            Set<Weapon.WeaponType> eligibleWeapons
    ) {
    }

    private Set<WeaponType> getPolearmGatedWeapons(int level, WeaponType... baseWeapons) {
        Set<WeaponType> weapons = new HashSet<>(Set.of(baseWeapons));
        if (level >= 20) {
            weapons.add(WeaponType.POLEARM);
        }
        return weapons;
    }

    private Set<ArmorType> getWarriorArmorForLevel(int level) {
        return level >= ARMOR_TRANSITION_LEVEL
                ? Set.of(ArmorType.CLOTH, ArmorType.LEATHER, ArmorType.MAIL, ArmorType.PLATE)
                : Set.of(ArmorType.CLOTH, ArmorType.LEATHER, ArmorType.MAIL);
    }

    private Set<ArmorType> getPaladinArmorForLevel(int level) {
        return level >= ARMOR_TRANSITION_LEVEL
                ? Set.of(ArmorType.CLOTH, ArmorType.LEATHER, ArmorType.MAIL, ArmorType.PLATE)
                : Set.of(ArmorType.CLOTH, ArmorType.LEATHER, ArmorType.MAIL);
    }

    private Set<ArmorType> getHunterArmorForLevel(int level) {
        return level >= ARMOR_TRANSITION_LEVEL
                ? Set.of(ArmorType.CLOTH, ArmorType.LEATHER, ArmorType.MAIL)
                : Set.of(ArmorType.CLOTH, ArmorType.LEATHER);
    }

    private Set<ArmorType> getShamanArmorForLevel(int level) {
        return level >= ARMOR_TRANSITION_LEVEL
                ? Set.of(ArmorType.CLOTH, ArmorType.LEATHER, ArmorType.MAIL)
                : Set.of(ArmorType.CLOTH, ArmorType.LEATHER);
    }

}
