package com.bebis.BeBiS.engine.upgrade;

import com.bebis.BeBiS.engine.power.core.PowerCalculatorEngine;
import com.bebis.BeBiS.equipment.domain.EquippedItem;
import com.bebis.BeBiS.item.domain.Armor;
import com.bebis.BeBiS.item.domain.Item;
import com.bebis.BeBiS.profile.domain.ClassSpec;
import com.bebis.BeBiS.profile.domain.WowCharacter;
import com.bebis.BeBiS.profile.domain.WowRealm;
import com.bebis.BeBiS.profile.domain.WowTalents;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.bebis.BeBiS.profile.domain.WowTalents.Specialization;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class IsolatedUpgradeFinderTest {

    @Mock
    private PowerCalculatorEngine powerCalculator;

    private IsolatedUpgradeFinder finder;

    @BeforeEach
    void setup() {
        finder = new IsolatedUpgradeFinder(powerCalculator);
    }

    @Test
    void shouldSignalUpgradeFoundResult_EmptyEquippedItem() {
        // given
        WowCharacter wowCharacter = getWowCharacter();
        Optional<WowTalents> talents = Optional.of(getTalents());

        ClassSpec classSpec = ClassSpec.getClassSpec(wowCharacter.wowClass(), talents);

        Item worseItemMock = mock(Armor.class);
        Item betterItemMock = mock(Armor.class);

        when(powerCalculator.calculate(worseItemMock, classSpec, wowCharacter.race())).thenReturn(21.0);
        when(powerCalculator.calculate(betterItemMock, classSpec, wowCharacter.race())).thenReturn(37.0);

        List<Item> itemPool = List.of(worseItemMock, betterItemMock);

        // when
        UpgradeResult result = finder.findUpgrade(Optional.empty(), wowCharacter, talents, itemPool);

        // then
        assertThat(result).isInstanceOf(UpgradeResult.UpgradeFoundResult.class);
        UpgradeResult.UpgradeFoundResult upgradeFoundResult = (UpgradeResult.UpgradeFoundResult) result;

        assertThat(upgradeFoundResult.item()).isEqualTo(betterItemMock);

        // since equippedItem is null the finder should have only run calculation as many times as number of items in the pool
        verify(powerCalculator, times(itemPool.size())).calculate(any(Item.class), eq(classSpec), eq(wowCharacter.race()));
    }

    @Test
    void shouldSignalNoItemsAvailableResult_EmptyItemPool() {
        // given
        WowCharacter wowCharacter = getWowCharacter();
        Optional<WowTalents> talents = Optional.of(getTalents());

        ClassSpec classSpec = ClassSpec.getClassSpec(wowCharacter.wowClass(), talents);

        List<Item> itemPool = List.of();

        // when
        UpgradeResult result = finder.findUpgrade(Optional.empty(), wowCharacter, talents, itemPool);

        // then
        assertThat(result).isInstanceOf(UpgradeResult.NoItemsAvailableResult.class);

        verify(powerCalculator, never()).calculate(any(Item.class), eq(classSpec), eq(wowCharacter.race()));
    }

    @Test
    void shouldSignalUpgradeFoundResult_EquippedItemWorseThanBestItemInPool() {
        // given
        WowCharacter wowCharacter = getWowCharacter();
        Optional<WowTalents> talents = Optional.of(getTalents());

        ClassSpec classSpec = ClassSpec.getClassSpec(wowCharacter.wowClass(), talents);

        EquippedItem equippedItemMock = mock(EquippedItem.class);
        Item currentItemMock = mock(Armor.class);
        when(equippedItemMock.item()).thenReturn(currentItemMock);

        Item otherItemMock1 = mock(Armor.class);
        Item bestItemMock = mock(Armor.class);

        when(powerCalculator.calculate(currentItemMock, classSpec, wowCharacter.race())).thenReturn(21.0);
        when(powerCalculator.calculate(otherItemMock1, classSpec, wowCharacter.race())).thenReturn(37.0);
        when(powerCalculator.calculate(bestItemMock, classSpec, wowCharacter.race())).thenReturn(69.0);

        List<Item> itemPool = List.of(currentItemMock, otherItemMock1, bestItemMock);

        // when
        UpgradeResult result = finder.findUpgrade(Optional.of(equippedItemMock), wowCharacter, talents, itemPool);

        // then
        assertThat(result).isInstanceOf(UpgradeResult.UpgradeFoundResult.class);
        UpgradeResult.UpgradeFoundResult upgradeFoundResult = (UpgradeResult.UpgradeFoundResult) result;

        assertThat(upgradeFoundResult.item()).isEqualTo(bestItemMock);

        // additional 1 calculation for the equipped item comparison logic
        verify(powerCalculator, times(itemPool.size() + 1)).calculate(any(Item.class), eq(classSpec), eq(wowCharacter.race()));
    }

    @ParameterizedTest
    @MethodSource("scoreProvider")
    void shouldSignalAlreadyBiSResult_EquippedItemIsBiSOrEqualToAnotherItemInPool(double eqItemScore, double otherItemScore) {
        // given
        WowCharacter wowCharacter = getWowCharacter();
        Optional<WowTalents> talents = Optional.of(getTalents());

        ClassSpec classSpec = ClassSpec.getClassSpec(wowCharacter.wowClass(), talents);

        EquippedItem equippedItemMock = mock(EquippedItem.class);
        Item currentItemMock = mock(Armor.class);
        when(equippedItemMock.item()).thenReturn(currentItemMock);

        Item otherItemMock = mock(Armor.class);

        when(powerCalculator.calculate(currentItemMock, classSpec, wowCharacter.race())).thenReturn(eqItemScore);
        when(powerCalculator.calculate(otherItemMock, classSpec, wowCharacter.race())).thenReturn(otherItemScore);

        List<Item> itemPool = List.of(currentItemMock, otherItemMock);

        // when
        UpgradeResult result = finder.findUpgrade(Optional.of(equippedItemMock), wowCharacter, talents, itemPool);

        // then
        assertThat(result).isInstanceOf(UpgradeResult.AlreadyBiSResult.class);

        // additional 1 calculation for the equipped item comparison logic
        verify(powerCalculator, times(itemPool.size() + 1)).calculate(any(Item.class), eq(classSpec), eq(wowCharacter.race()));
    }

    private static Stream<Arguments> scoreProvider() {
        return Stream.of(
                Arguments.of(37.0, 21.0),
                Arguments.of(21.0, 21.0)
        );
    }

    private WowCharacter getWowCharacter() {
        return new WowCharacter(
                new WowCharacter.Id(1L, "soulseeker"),
                "Thelamar",
                60,
                WowCharacter.Race.NIGHT_ELF,
                WowCharacter.WowClass.ROGUE,
                new WowRealm("Soulseeker", "soulseeker")
        );
    }

    private WowTalents getTalents() {
        return new WowTalents(
                List.of(
                        new Specialization("COMBAT", 31),
                        new Specialization("ASSASSINATION", 20),
                        new Specialization("SUBTLETY", 0)
                ));
    }
}
