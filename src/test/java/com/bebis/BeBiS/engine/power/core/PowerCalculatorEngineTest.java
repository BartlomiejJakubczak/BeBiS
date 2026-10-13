package com.bebis.BeBiS.engine.power.core;

import com.bebis.BeBiS.equipment.domain.Equipment;
import com.bebis.BeBiS.item.ArmorBuilder;
import com.bebis.BeBiS.item.EquippableItemBuilder;
import com.bebis.BeBiS.item.WeaponBuilder;
import com.bebis.BeBiS.item.domain.Armor;
import com.bebis.BeBiS.item.domain.EquippableItem;
import com.bebis.BeBiS.item.domain.Weapon;
import com.bebis.BeBiS.profile.domain.ClassSpec;
import com.bebis.BeBiS.profile.domain.WowCharacter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class PowerCalculatorEngineTest {

    private PowerCalculatorEngine calculator;

    @Mock
    private AttributeScoreCalculator attributeCalculator;

    @Mock
    private WeaponScoreCalculator weaponCalculator;

    @BeforeEach
    void setup() {
        calculator = new PowerCalculatorEngine(attributeCalculator, weaponCalculator);
    }

    @Test
    void shouldDelegateWeaponCalculationToWeaponCalculator() {
        // Given
        Weapon weapon = WeaponBuilder.newOneHandSword().build();
        Equipment.Slot slot = Equipment.Slot.MAIN_HAND;
        ClassSpec classSpec = ClassSpec.WARRIOR_ARMS;
        WowCharacter.Race race = WowCharacter.Race.ORC;

        when(weaponCalculator.calculate(eq(slot), any(Weapon.class), eq(classSpec), eq(race)))
                .thenReturn(150.0);

        // When
        double score = calculator.calculate(slot, weapon, classSpec, race);

        // Then
        assertThat(score).isEqualTo(150.0);
        verify(weaponCalculator).calculate(eq(slot),
                argThat(w -> w.getMetadata().name().equals(weapon.getMetadata().name())), eq(classSpec), eq(race));
    }

    @Test
    void shouldDelegateArmorCalculationToAttributeCalculator() {
        // Given
        Armor armor = ArmorBuilder.newShield().build();
        Equipment.Slot slot = Equipment.Slot.OFF_HAND;
        ClassSpec classSpec = ClassSpec.WARRIOR_PROTECTION;
        WowCharacter.Race race = WowCharacter.Race.HUMAN;

        when(attributeCalculator.calculate(any(Armor.class), eq(classSpec))).thenReturn(85.5);

        // When
        double score = calculator.calculate(slot, armor, classSpec, race);

        // Then
        assertThat(score).isEqualTo(85.5);
        verify(attributeCalculator).calculate(argThat(a -> a.getMetadata().name().equals(armor.getMetadata().name())), eq(classSpec));
    }

    @Test
    void shouldDelegateSimpleItemCalculationToAttributeCalculator() {
        // Given
        EquippableItem ring = EquippableItemBuilder.newRing().build();
        ClassSpec classSpec = ClassSpec.ROGUE_COMBAT;

        when(attributeCalculator.calculate(any(EquippableItem.class), eq(classSpec))).thenReturn(60.0);

        // When
        double score = calculator.calculate(ring, classSpec);

        // Then
        assertThat(score).isEqualTo(60.0);
        verify(attributeCalculator).calculate(argThat(r -> r.getMetadata().name().equals(ring.getMetadata().name())), eq(classSpec));
    }

}
