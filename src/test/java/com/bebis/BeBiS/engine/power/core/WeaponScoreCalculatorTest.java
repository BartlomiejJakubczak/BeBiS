package com.bebis.BeBiS.engine.power.core;

import com.bebis.BeBiS.engine.power.domain.StatWeights;
import com.bebis.BeBiS.equipment.domain.Equipment.Slot;
import com.bebis.BeBiS.item.WeaponBuilder;
import com.bebis.BeBiS.item.domain.StatType;
import com.bebis.BeBiS.item.domain.Weapon;
import com.bebis.BeBiS.profile.domain.ClassSpec;
import com.bebis.BeBiS.profile.domain.WowCharacter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.stream.Stream;

import static com.bebis.BeBiS.item.domain.StatType.*;
import static com.bebis.BeBiS.profile.domain.ClassSpec.HUNTER_MARKSMANSHIP;
import static com.bebis.BeBiS.profile.domain.ClassSpec.WARRIOR_FURY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class WeaponScoreCalculatorTest {

    private static final double RANGED_FLAT_BONUS = 20.0;
    private static final double MELEE_FLAT_BONUS = 80.0;

    @Mock
    private WeaponStatWeightRegistry registry;

    @Mock
    private AttributeScoreCalculator attributeCalculator;

    private WeaponScoreCalculator calculator;

    // test data
    private final ClassSpec testClassSpec = WARRIOR_FURY;

    private final Map<StatType, Double> testWeights = Map.of(
            MAIN_HAND_DPS, 16.5, MAIN_HAND_SPEED, 12.0,
            OFF_HAND_DPS, 8.25, OFF_HAND_SPEED, -5.0,
            TWO_HAND_DPS, 15.0, TWO_HAND_SPEED, 15.0);

    private final StatWeights testStatWeights = new StatWeights(testWeights);

    private final WowCharacter.Race neutralRace = WowCharacter.Race.NIGHT_ELF; // night-elfs don't have weapon skill bonuses

    @BeforeEach
    void setup() {
        calculator = new WeaponScoreCalculator(registry, attributeCalculator);
    }

    @ParameterizedTest
    @MethodSource("provideRacialWeaponSkillBonuses")
    void shouldApplyRacialWeaponSkillBonuses(WowCharacter.Race bonusRace, Weapon.WeaponType bonusType, double expectedBonus) {
        // given
        when(registry.getWeightsFor(eq(testClassSpec))).thenReturn(testStatWeights);

        Weapon weapon = WeaponBuilder.newOneHandSword()
                .withWeaponType(bonusType.name())
                .build();

        when(attributeCalculator.calculate(weapon, testClassSpec)).thenReturn(21.37);

        Slot slot = Slot.MAIN_HAND;
        double neutralRaceWeaponScore = calculator.calculate(slot, weapon, testClassSpec, neutralRace);

        // when
        double bonusWeaponScore = calculator.calculate(slot, weapon, testClassSpec, bonusRace);

        // then
        assertThat(bonusWeaponScore - neutralRaceWeaponScore).isEqualTo(expectedBonus);
    }

    private static Stream<Arguments> provideRacialWeaponSkillBonuses() {
        return Stream.of(
                Arguments.of(WowCharacter.Race.ORC, Weapon.WeaponType.AXE, MELEE_FLAT_BONUS),
                Arguments.of(WowCharacter.Race.HUMAN, Weapon.WeaponType.SWORD, MELEE_FLAT_BONUS),
                Arguments.of(WowCharacter.Race.HUMAN, Weapon.WeaponType.MACE, MELEE_FLAT_BONUS),
                Arguments.of(WowCharacter.Race.DWARF, Weapon.WeaponType.GUN, RANGED_FLAT_BONUS),
                Arguments.of(WowCharacter.Race.TROLL, Weapon.WeaponType.BOW, RANGED_FLAT_BONUS)
        );
    }

    @Test
    void shouldTakeIntoAccountWeaponSpecifics_TwoHand() {
        // given
        when(registry.getWeightsFor(eq(testClassSpec))).thenReturn(testStatWeights);

        Weapon twoHand = WeaponBuilder.newOneHandSword()
                .withInventoryType("TWO_HAND")
                .withMinDamage(100)
                .withMaxDamage(200)
                .withSpeed(2.0)
                .build();

        double expectedStatsScore = 30.0;
        when(attributeCalculator.calculate(twoHand, testClassSpec)).thenReturn(expectedStatsScore);

        double expectedRacialScore = 0.0;
        double expectedDpsScore = calculateDps(twoHand.getMinDamage(), twoHand.getMaxDamage(), twoHand.getSpeed()) * testWeights.get(TWO_HAND_DPS);
        double expectedSpeedScore = twoHand.getSpeed() * testWeights.get(TWO_HAND_SPEED);
        double expectedTotalScore = expectedStatsScore + expectedRacialScore + expectedDpsScore + expectedSpeedScore;

        // when
        double result = calculator.calculate(Slot.MAIN_HAND, twoHand, testClassSpec, neutralRace);

        // then
        verify(attributeCalculator).calculate(eq(twoHand), eq(testClassSpec));

        assertThat(result).isEqualTo(expectedTotalScore);
    }

    @Test
    void shouldTakeIntoAccountWeaponSpecifics_OneHand_MainHand() {
        // given
        when(registry.getWeightsFor(eq(testClassSpec))).thenReturn(testStatWeights);

        Weapon oneHand = WeaponBuilder.newOneHandSword().build();

        double expectedStatsScore = 30.0;
        when(attributeCalculator.calculate(oneHand, testClassSpec)).thenReturn(expectedStatsScore);

        double expectedRacialScore = 0.0;
        double expectedDpsScore = calculateDps(oneHand.getMinDamage(), oneHand.getMaxDamage(), oneHand.getSpeed()) * testWeights.get(MAIN_HAND_DPS);
        double expectedSpeedScore = oneHand.getSpeed() * testWeights.get(MAIN_HAND_SPEED);
        double expectedTotalScore = expectedStatsScore + expectedRacialScore + expectedDpsScore + expectedSpeedScore;

        // when
        double result = calculator.calculate(Slot.MAIN_HAND, oneHand, testClassSpec, neutralRace);

        // then
        verify(attributeCalculator).calculate(eq(oneHand), eq(testClassSpec));

        assertThat(result).isEqualTo(expectedTotalScore);
    }

    @Test
    void shouldTakeIntoAccountWeaponSpecifics_OneHand_OffHand() {
        // given
        when(registry.getWeightsFor(eq(testClassSpec))).thenReturn(testStatWeights);

        Weapon oneHand = WeaponBuilder.newOneHandSword().build();

        double expectedStatsScore = 30.0;
        when(attributeCalculator.calculate(oneHand, testClassSpec)).thenReturn(expectedStatsScore);

        double expectedRacialScore = 0.0;
        double expectedDpsScore = calculateDps(oneHand.getMinDamage(), oneHand.getMaxDamage(), oneHand.getSpeed()) * testWeights.get(OFF_HAND_DPS);
        double expectedSpeedScore = oneHand.getSpeed() * testWeights.get(OFF_HAND_SPEED);
        double expectedTotalScore = expectedStatsScore + expectedRacialScore + expectedDpsScore + expectedSpeedScore;

        // when
        double result = calculator.calculate(Slot.OFF_HAND, oneHand, testClassSpec, neutralRace);

        // then
        verify(attributeCalculator).calculate(eq(oneHand), eq(testClassSpec));

        assertThat(result).isEqualTo(expectedTotalScore);
    }

    @Test
    void shouldTakeIntoAccountWeaponSpecifics_Ranged() {
        // given
        ClassSpec testClassSpec = HUNTER_MARKSMANSHIP;

        Map<StatType, Double> testWeights = Map.of(RANGED_WEAPON_DPS, 20.0, RANGED_WEAPON_SPEED, 15.0);

        when(registry.getWeightsFor(eq(testClassSpec))).thenReturn(new StatWeights(testWeights));

        Weapon ranged = WeaponBuilder.newRanged().build();

        double expectedStatsScore = 40.0;
        when(attributeCalculator.calculate(ranged, testClassSpec)).thenReturn(expectedStatsScore);

        double expectedRacialScore = 0.0;
        double expectedDpsScore = calculateDps(ranged.getMinDamage(), ranged.getMaxDamage(), ranged.getSpeed()) * testWeights.get(RANGED_WEAPON_DPS);
        double expectedSpeedScore = ranged.getSpeed() * testWeights.get(RANGED_WEAPON_SPEED);
        double expectedTotalScore = expectedStatsScore + expectedRacialScore + expectedDpsScore + expectedSpeedScore;

        // when
        double result = calculator.calculate(Slot.RANGED, ranged, testClassSpec, neutralRace);

        // then
        verify(attributeCalculator).calculate(eq(ranged), eq(testClassSpec));

        assertThat(result).isEqualTo(expectedTotalScore);
    }

    private double calculateDps(int minDamage, int maxDamage, double speed) {
        return ((maxDamage + minDamage) / 2.0) / speed;
    }
}
