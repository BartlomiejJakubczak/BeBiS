package com.bebis.BeBiS.engine.upgrade;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Set;
import java.util.stream.Stream;

import static com.bebis.BeBiS.engine.upgrade.ClassItemTypeCapabilitiesResolver.ClassItemTypeCapabilities;
import static com.bebis.BeBiS.item.domain.Armor.ArmorType;
import static com.bebis.BeBiS.item.domain.Weapon.WeaponType;
import static com.bebis.BeBiS.profile.domain.WowCharacter.WowClass;
import static org.assertj.core.api.Assertions.assertThat;

public class ClassItemTypeCapabilitiesResolverTest {

    private final ClassItemTypeCapabilitiesResolver resolver = new ClassItemTypeCapabilitiesResolver();

    @ParameterizedTest
    @MethodSource("armorProgressionProvider")
    void shouldResolveCorrectArmorTypesBasedOnLevel(WowClass wowClass, int level, Set<ArmorType> expectedArmor) {
        // when
        ClassItemTypeCapabilities capabilities = resolver.getCapabilitiesFor(wowClass, level);

        // then
        assertThat(capabilities.eligibleArmor()).containsExactlyInAnyOrderElementsOf(expectedArmor);
    }

    private static Stream<Arguments> armorProgressionProvider() {
        return Stream.of(
                // Level 39 Armor Restrictions
                Arguments.of(WowClass.WARRIOR, 39, Set.of(ArmorType.CLOTH, ArmorType.LEATHER, ArmorType.MAIL)),
                Arguments.of(WowClass.PALADIN, 39, Set.of(ArmorType.CLOTH, ArmorType.LEATHER, ArmorType.MAIL)),
                Arguments.of(WowClass.HUNTER, 39, Set.of(ArmorType.CLOTH, ArmorType.LEATHER)),
                Arguments.of(WowClass.SHAMAN, 39, Set.of(ArmorType.CLOTH, ArmorType.LEATHER)),

                // Level 40 Armor Upgrades
                Arguments.of(WowClass.WARRIOR, 40, Set.of(ArmorType.CLOTH, ArmorType.LEATHER, ArmorType.MAIL, ArmorType.PLATE)),
                Arguments.of(WowClass.PALADIN, 40, Set.of(ArmorType.CLOTH, ArmorType.LEATHER, ArmorType.MAIL, ArmorType.PLATE)),
                Arguments.of(WowClass.HUNTER, 40, Set.of(ArmorType.CLOTH, ArmorType.LEATHER, ArmorType.MAIL)),
                Arguments.of(WowClass.SHAMAN, 40, Set.of(ArmorType.CLOTH, ArmorType.LEATHER, ArmorType.MAIL)),

                // Static Armor Classes
                Arguments.of(WowClass.ROGUE, 1, Set.of(ArmorType.CLOTH, ArmorType.LEATHER)),
                Arguments.of(WowClass.ROGUE, 60, Set.of(ArmorType.CLOTH, ArmorType.LEATHER)),
                Arguments.of(WowClass.DRUID, 1, Set.of(ArmorType.CLOTH, ArmorType.LEATHER)),
                Arguments.of(WowClass.MAGE, 60, Set.of(ArmorType.CLOTH)),
                Arguments.of(WowClass.WARLOCK, 60, Set.of(ArmorType.CLOTH)),
                Arguments.of(WowClass.PRIEST, 60, Set.of(ArmorType.CLOTH))
        );
    }

    @ParameterizedTest
    @MethodSource("staticWeaponProvider")
    void shouldResolveCorrectWeaponsForStaticClasses(WowClass wowClass, Set<WeaponType> expectedWeapons) {
        // when
        ClassItemTypeCapabilities capabilities = resolver.getCapabilitiesFor(wowClass, 60);

        // then
        assertThat(capabilities.eligibleWeapons()).containsExactlyInAnyOrderElementsOf(expectedWeapons);
    }

    private static Stream<Arguments> staticWeaponProvider() {
        return Stream.of(
                Arguments.of(WowClass.SHAMAN, Set.of(WeaponType.MACE, WeaponType.AXE, WeaponType.STAFF, WeaponType.DAGGER, WeaponType.UNARMED)),
                Arguments.of(WowClass.ROGUE, Set.of(WeaponType.SWORD, WeaponType.DAGGER, WeaponType.MACE, WeaponType.BOW, WeaponType.CROSSBOW, WeaponType.GUN, WeaponType.UNARMED)),
                Arguments.of(WowClass.DRUID, Set.of(WeaponType.STAFF, WeaponType.MACE, WeaponType.DAGGER, WeaponType.UNARMED)),
                Arguments.of(WowClass.MAGE, Set.of(WeaponType.WAND, WeaponType.STAFF, WeaponType.DAGGER, WeaponType.SWORD, WeaponType.UNARMED)),
                Arguments.of(WowClass.WARLOCK, Set.of(WeaponType.WAND, WeaponType.STAFF, WeaponType.DAGGER, WeaponType.SWORD, WeaponType.UNARMED)),
                Arguments.of(WowClass.PRIEST, Set.of(WeaponType.WAND, WeaponType.STAFF, WeaponType.DAGGER, WeaponType.MACE, WeaponType.UNARMED))
        );
    }

    @ParameterizedTest
    @EnumSource(value = WowClass.class, names = {"WARRIOR", "PALADIN", "HUNTER"})
    void shouldResolveSelectClassesWeaponsIncludingPolearmsAtHighLevel(WowClass wowClass) {
        // when
        ClassItemTypeCapabilities capabilities = resolver.getCapabilitiesFor(wowClass, 25);

        // then
        assertThat(capabilities.eligibleWeapons()).contains(WeaponType.POLEARM);
    }

}
