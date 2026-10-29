package com.bebis.BeBiS.profile.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.bebis.BeBiS.profile.domain.ClassSpec.getClassSpec;
import static com.bebis.BeBiS.profile.domain.WowCharacter.WowClass;
import static com.bebis.BeBiS.profile.domain.WowTalentsBuilder.newWowTalents;
import static org.assertj.core.api.Assertions.assertThat;

public class ClassSpecTest {

    @ParameterizedTest
    @MethodSource("classSpecsProvider")
    void shouldGetExactClassSpecCorrectly(WowClass wowClass, WowTalents talents, ClassSpec expectedSpec) {
        // when
        ClassSpec actualSpec = getClassSpec(wowClass, talents);

        // then
        assertThat(actualSpec).isEqualTo(expectedSpec);
    }

    private static WowTalents getWowTalentsWithMostPointsIn(String specName) {
        return newWowTalents().withSpec(specName, 31).withSpec("SPEC_2", 20).build();
    }

    private static Stream<Arguments> classSpecsProvider() {
        return Stream.of(
                // Warrior
                Arguments.of(WowClass.WARRIOR, getWowTalentsWithMostPointsIn("ARMS"), ClassSpec.WARRIOR_ARMS),
                Arguments.of(WowClass.WARRIOR, getWowTalentsWithMostPointsIn("FURY"), ClassSpec.WARRIOR_FURY),
                Arguments.of(WowClass.WARRIOR, getWowTalentsWithMostPointsIn("PROTECTION"), ClassSpec.WARRIOR_PROTECTION),

                // Paladin
                Arguments.of(WowClass.PALADIN, getWowTalentsWithMostPointsIn("HOLY"), ClassSpec.PALADIN_HOLY),
                Arguments.of(WowClass.PALADIN, getWowTalentsWithMostPointsIn("PROTECTION"), ClassSpec.PALADIN_PROTECTION),
                Arguments.of(WowClass.PALADIN, getWowTalentsWithMostPointsIn("RETRIBUTION"), ClassSpec.PALADIN_RETRIBUTION),

                // Rogue
                Arguments.of(WowClass.ROGUE, getWowTalentsWithMostPointsIn("ASSASSINATION"), ClassSpec.ROGUE_ASSASSINATION),
                Arguments.of(WowClass.ROGUE, getWowTalentsWithMostPointsIn("COMBAT"), ClassSpec.ROGUE_COMBAT),
                Arguments.of(WowClass.ROGUE, getWowTalentsWithMostPointsIn("SUBTLETY"), ClassSpec.ROGUE_SUBTLETY),

                // Shaman
                Arguments.of(WowClass.SHAMAN, getWowTalentsWithMostPointsIn("ELEMENTAL"), ClassSpec.SHAMAN_ELEMENTAL),
                Arguments.of(WowClass.SHAMAN, getWowTalentsWithMostPointsIn("ENHANCEMENT"), ClassSpec.SHAMAN_ENHANCEMENT),
                Arguments.of(WowClass.SHAMAN, getWowTalentsWithMostPointsIn("RESTORATION"), ClassSpec.SHAMAN_RESTORATION),

                // Druid
                Arguments.of(WowClass.DRUID, getWowTalentsWithMostPointsIn("BALANCE"), ClassSpec.DRUID_BALANCE),
                Arguments.of(WowClass.DRUID, getWowTalentsWithMostPointsIn("FERALCOMBAT"), ClassSpec.DRUID_FERAL),
                Arguments.of(WowClass.DRUID, getWowTalentsWithMostPointsIn("RESTORATION"), ClassSpec.DRUID_RESTORATION),

                // Hunter
                Arguments.of(WowClass.HUNTER, getWowTalentsWithMostPointsIn("BEASTMASTERY"), ClassSpec.HUNTER_BEASTMASTERY),
                Arguments.of(WowClass.HUNTER, getWowTalentsWithMostPointsIn("MARKSMANSHIP"), ClassSpec.HUNTER_MARKSMANSHIP),
                Arguments.of(WowClass.HUNTER, getWowTalentsWithMostPointsIn("SURVIVAL"), ClassSpec.HUNTER_SURVIVAL),

                // Priest
                Arguments.of(WowClass.PRIEST, getWowTalentsWithMostPointsIn("DISCIPLINE"), ClassSpec.PRIEST_DISCIPLINE),
                Arguments.of(WowClass.PRIEST, getWowTalentsWithMostPointsIn("HOLY"), ClassSpec.PRIEST_HOLY),
                Arguments.of(WowClass.PRIEST, getWowTalentsWithMostPointsIn("SHADOW"), ClassSpec.PRIEST_SHADOW),

                // Mage
                Arguments.of(WowClass.MAGE, getWowTalentsWithMostPointsIn("ARCANE"), ClassSpec.MAGE_ARCANE),
                Arguments.of(WowClass.MAGE, getWowTalentsWithMostPointsIn("FIRE"), ClassSpec.MAGE_FIRE),
                Arguments.of(WowClass.MAGE, getWowTalentsWithMostPointsIn("FROST"), ClassSpec.MAGE_FROST),

                // Warlock
                Arguments.of(WowClass.WARLOCK, getWowTalentsWithMostPointsIn("AFFLICTION"), ClassSpec.WARLOCK_AFFLICTION),
                Arguments.of(WowClass.WARLOCK, getWowTalentsWithMostPointsIn("DEMONOLOGY"), ClassSpec.WARLOCK_DEMONOLOGY),
                Arguments.of(WowClass.WARLOCK, getWowTalentsWithMostPointsIn("DESTRUCTION"), ClassSpec.WARLOCK_DESTRUCTION),

                // Base Baseline Defaults (Fresh characters/Respecs)
                Arguments.of(WowClass.WARRIOR, newWowTalents().build(), ClassSpec.WARRIOR_NONE),
                Arguments.of(WowClass.PALADIN, newWowTalents().build(), ClassSpec.PALADIN_NONE),
                Arguments.of(WowClass.ROGUE, newWowTalents().build(), ClassSpec.ROGUE_NONE),
                Arguments.of(WowClass.SHAMAN, newWowTalents().build(), ClassSpec.SHAMAN_NONE),
                Arguments.of(WowClass.DRUID, newWowTalents().build(), ClassSpec.DRUID_NONE),
                Arguments.of(WowClass.HUNTER, newWowTalents().build(), ClassSpec.HUNTER_NONE),
                Arguments.of(WowClass.PRIEST, newWowTalents().build(), ClassSpec.PRIEST_NONE),
                Arguments.of(WowClass.MAGE, newWowTalents().build(), ClassSpec.MAGE_NONE),
                Arguments.of(WowClass.WARLOCK, newWowTalents().build(), ClassSpec.WARLOCK_NONE)
        );
    }

    @ParameterizedTest
    @MethodSource("corruptedClassSpecsProvider")
    void shouldFallbackToNoneSuffixedClassSpecWhenDataIsCorrupted(WowClass wowClass, WowTalents talents, ClassSpec expectedFallback) {
        // when
        ClassSpec actualSpec = getClassSpec(wowClass, talents);

        // then
        assertThat(actualSpec).isEqualTo(expectedFallback);
        assertThat(actualSpec.name()).endsWith("_NONE");
    }

    private static Stream<Arguments> corruptedClassSpecsProvider() {
        return Stream.of(
                Arguments.of(WowClass.WARRIOR, getWowTalentsWithMostPointsIn("FLURY"), ClassSpec.WARRIOR_NONE),
                Arguments.of(WowClass.PALADIN, getWowTalentsWithMostPointsIn("UNHOLY"), ClassSpec.PALADIN_NONE),
                Arguments.of(WowClass.ROGUE, getWowTalentsWithMostPointsIn("SUBTLTY"), ClassSpec.ROGUE_NONE),
                Arguments.of(WowClass.SHAMAN, getWowTalentsWithMostPointsIn("ENCH"), ClassSpec.SHAMAN_NONE),
                Arguments.of(WowClass.DRUID, getWowTalentsWithMostPointsIn("RESTO"), ClassSpec.DRUID_NONE),
                Arguments.of(WowClass.HUNTER, getWowTalentsWithMostPointsIn("MARKSMAN"), ClassSpec.HUNTER_NONE),
                Arguments.of(WowClass.PRIEST, getWowTalentsWithMostPointsIn("DISC"), ClassSpec.PRIEST_NONE),
                Arguments.of(WowClass.MAGE, getWowTalentsWithMostPointsIn("PROST"), ClassSpec.MAGE_NONE),
                Arguments.of(WowClass.WARLOCK, getWowTalentsWithMostPointsIn("DEMO"), ClassSpec.WARLOCK_NONE),

                // Cross-class mismatch trap (e.g. Scraper gets a Priest with "FIRE")
                Arguments.of(WowClass.PRIEST, getWowTalentsWithMostPointsIn("FIRE"), ClassSpec.PRIEST_NONE)
        );
    }

}
