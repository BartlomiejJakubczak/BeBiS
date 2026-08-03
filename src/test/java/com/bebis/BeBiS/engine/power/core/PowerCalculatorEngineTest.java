package com.bebis.BeBiS.engine.power.core;

import com.bebis.BeBiS.engine.power.domain.StatWeights;
import com.bebis.BeBiS.item.domain.Item;
import com.bebis.BeBiS.item.domain.StatType;
import com.bebis.BeBiS.item.domain.Weapon;
import com.bebis.BeBiS.profile.domain.ClassSpec;
import com.bebis.BeBiS.profile.domain.WowCharacter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class PowerCalculatorEngineTest {

    private PowerCalculatorEngine calculator;

    @Mock
    private StatWeightRegistry registry;

    @BeforeEach
    void setup() {
        calculator = new PowerCalculatorEngine(registry);
    }

    @ParameterizedTest
    @MethodSource("provideRacialWeaponSkillBonuses")
    void shouldApplyRacialWeaponSkillBonuses(WowCharacter.Race race, Weapon.WeaponType bonusType, double expectedBonus) {
        // given
        Map<StatType, Double> testWeights = Map.of(
                StatType.WEAPON_SPEED, 10.0,
                StatType.WEAPON_DPS, 5.0
        );
        StatWeights testStatWeights = new StatWeights(testWeights);

        when(registry.getWeightsFor(any(ClassSpec.class))).thenReturn(testStatWeights); // classSpec is irrelevant for this test

        Weapon testWeapon = generateWeapon(bonusType, 1.5, 21, 37);

        // when / then
        WowCharacter.Race neutralRace = WowCharacter.Race.NIGHT_ELF; // night-elfs don't have weapon skill bonuses
        ClassSpec testClassSpec = ClassSpec.WARRIOR_ARMS;

        double neutralRaceWeaponScore = calculator.calculate(testWeapon, testClassSpec, neutralRace);
        double bonusWeaponScore = calculator.calculate(testWeapon, testClassSpec, race);

        assertThat(bonusWeaponScore - neutralRaceWeaponScore).isEqualTo(expectedBonus);
    }

    private static Stream<Arguments> provideRacialWeaponSkillBonuses() {
        return Stream.of(
                Arguments.of(WowCharacter.Race.ORC, Weapon.WeaponType.AXE, PowerCalculatorEngine.MELEE_FLAT_BONUS),
                Arguments.of(WowCharacter.Race.HUMAN, Weapon.WeaponType.SWORD, PowerCalculatorEngine.MELEE_FLAT_BONUS),
                Arguments.of(WowCharacter.Race.HUMAN, Weapon.WeaponType.MACE, PowerCalculatorEngine.MELEE_FLAT_BONUS),
                Arguments.of(WowCharacter.Race.DWARF, Weapon.WeaponType.GUN, PowerCalculatorEngine.RANGED_FLAT_BONUS),
                Arguments.of(WowCharacter.Race.TROLL, Weapon.WeaponType.BOW, PowerCalculatorEngine.RANGED_FLAT_BONUS)
        );
    }

    @ParameterizedTest
    @ValueSource(doubles = {9.0, 0.0, -9.0})
    void shouldTakeIntoAccountWeaponSpeedPreferences(double weight) {
        // given
        Map<StatType, Double> protWeights = Map.of(
                StatType.WEAPON_SPEED, weight
        );
        StatWeights testStatWeights = new StatWeights(protWeights);

        ClassSpec testClassSpec = ClassSpec.WARRIOR_PROTECTION;
        when(registry.getWeightsFor(any(ClassSpec.class))).thenReturn(testStatWeights);

        // the weapons should have the same dps
        Weapon slowWeapon = generateWeapon(Weapon.WeaponType.SWORD, 3.0, 30, 30);
        Weapon fastWeapon = generateWeapon(Weapon.WeaponType.DAGGER, 1.5, 15, 15);

        // when/then
        WowCharacter.Race neutralRace = WowCharacter.Race.NIGHT_ELF;

        double slowScore = calculator.calculate(slowWeapon, testClassSpec, neutralRace);
        double fastScore = calculator.calculate(fastWeapon, testClassSpec, neutralRace);

        switch (Double.compare(weight, 0.0)) {
            case 1 -> assertThat(slowScore).isGreaterThan(fastScore); // slow means more speed value
            case 0 -> assertThat(fastScore).isEqualTo(slowScore);
            case -1 -> assertThat(fastScore).isGreaterThan(slowScore); // fast means less speed value
        }
    }

    @ParameterizedTest
    @ValueSource(doubles = {9.0, 0.0, -9.0})
    void shouldTakeIntoAccountWeaponDpsPreferences(double weight) {
        // given
        Map<StatType, Double> protWeights = Map.of(
                StatType.WEAPON_DPS, weight
        );
        StatWeights testStatWeights = new StatWeights(protWeights);

        ClassSpec testClassSpec = ClassSpec.WARRIOR_FURY;
        when(registry.getWeightsFor(any(ClassSpec.class))).thenReturn(testStatWeights);

        // the weapons should have the same speed
        Weapon lowDpsWeapon = generateWeapon(Weapon.WeaponType.SWORD, 2.0, 20, 20);
        Weapon highDpsWeapon = generateWeapon(Weapon.WeaponType.SWORD, 2.0, 40, 40);

        // when/then
        WowCharacter.Race neutralRace = WowCharacter.Race.NIGHT_ELF;

        double lowDpsScore = calculator.calculate(lowDpsWeapon, testClassSpec, neutralRace);
        double highDpsScore = calculator.calculate(highDpsWeapon, testClassSpec, neutralRace);

        switch (Double.compare(weight, 0.0)) {
            case 1 -> assertThat(highDpsScore).isGreaterThan(lowDpsScore);
            case 0 -> assertThat(highDpsScore).isEqualTo(lowDpsScore);
            case -1 -> assertThat(lowDpsScore).isGreaterThan(highDpsScore);
        }
    }

    private Weapon generateWeapon(Weapon.WeaponType weaponType, Double speed, int minDamage, int maxDamage) {
        Item.ItemMetadata metadata = new Item.ItemMetadata(
                new Item.ItemKey(21L, 37L), "Placeholder", Item.InventoryType.WEAPONMAINHAND, Item.Quality.COMMON, 21, 37, false, Map.of(), List.of()
        );
        return new Weapon(
                metadata, speed, minDamage, maxDamage, weaponType
        );
    }

}
