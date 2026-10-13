package com.bebis.BeBiS.engine.upgrade;

import com.bebis.BeBiS.engine.power.core.PowerCalculatorEngine;
import com.bebis.BeBiS.engine.upgrade.domain.EquippedItemPair;
import com.bebis.BeBiS.engine.upgrade.domain.HandSlotCombinations;
import com.bebis.BeBiS.engine.upgrade.domain.HandSlotTypes;
import com.bebis.BeBiS.engine.upgrade.domain.UpgradeResult;
import com.bebis.BeBiS.equipment.domain.Equipment;
import com.bebis.BeBiS.equipment.domain.EquippedItem;
import com.bebis.BeBiS.item.ArmorBuilder;
import com.bebis.BeBiS.item.EquippableItemBuilder;
import com.bebis.BeBiS.item.WeaponBuilder;
import com.bebis.BeBiS.item.domain.Armor;
import com.bebis.BeBiS.item.domain.Item;
import com.bebis.BeBiS.item.domain.Weapon;
import com.bebis.BeBiS.profile.domain.ClassSpec;
import com.bebis.BeBiS.profile.domain.WowCharacter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.bebis.BeBiS.engine.upgrade.HandSlotUpgradeOrchestrator.CharacterContext;
import static com.bebis.BeBiS.engine.upgrade.HandSlotUpgradeOrchestrator.WeaponItemPools;
import static com.bebis.BeBiS.equipment.domain.Equipment.Slot.MAIN_HAND;
import static com.bebis.BeBiS.equipment.domain.Equipment.Slot.OFF_HAND;
import static com.bebis.BeBiS.item.domain.Item.InventoryType.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class HandSlotUpgradeFinderTest {

    @Mock
    private PowerCalculatorEngine powerCalculator;

    private HandSlotUpgradeFinder finder;

    // test data
    private final CharacterContext defaultContext = new CharacterContext(ClassSpec.ROGUE_COMBAT, WowCharacter.Race.NIGHT_ELF);
    private final HandSlotCombinations defaultCombos = HandSlotCombinations.from(new HandSlotTypes(List.of(WEAPON, WEAPONMAINHAND), List.of(WEAPON, WEAPONOFFHAND)));

    @BeforeEach
    void setUp() {
        finder = new HandSlotUpgradeFinder(powerCalculator, new UpgradeResultEvaluator());
    }

    @Test
    void shouldSignalNoItemsAvailableResult_EmptyItemPools() {
        // given / when
        Map<Equipment.Slot, UpgradeResult> result = finder.findUpgrades(getHandSlotsEquippedPair(null, null),
                defaultContext, defaultCombos, new WeaponItemPools(List.of(), List.of()));
        // then
        assertThat(result).containsOnlyKeys(MAIN_HAND, OFF_HAND);
        assertThat(result.values()).hasOnlyElementsOfType(UpgradeResult.NoItemsAvailableResult.class);

        verify(powerCalculator, never()).calculate(any(Equipment.Slot.class), any(Item.class), any(ClassSpec.class), any(WowCharacter.Race.class));
    }

    @Test
    void shouldSignalUpgradeFoundResultAndSlotClearedResult_TwoHandStrongerThanCurrentCombination() {
        // given
        CharacterContext context = new CharacterContext(ClassSpec.WARRIOR_ARMS, WowCharacter.Race.ORC);
        HandSlotCombinations combos = HandSlotCombinations.from(new HandSlotTypes(List.of(TWO_HAND), List.of()),
                new HandSlotTypes(List.of(WEAPON, WEAPONMAINHAND), List.of(WEAPON, WEAPONOFFHAND, SHIELD)));

        Weapon twoHand = createPowerfulTwoHandSword();
        Weapon currentWeapon = WeaponBuilder.newOneHandSword().build();
        Armor currentShield = ArmorBuilder.newShield().build();

        when(powerCalculator.calculate(MAIN_HAND, twoHand, context.classSpec(), context.race())).thenReturn(250.0);
        when(powerCalculator.calculate(MAIN_HAND, currentWeapon, context.classSpec(), context.race())).thenReturn(50.0);
        // Warriors can DW, so a 1h sword also exists in the off-hand pool
        when(powerCalculator.calculate(OFF_HAND, currentWeapon, context.classSpec(), context.race())).thenReturn(25.0);
        when(powerCalculator.calculate(OFF_HAND, currentShield, context.classSpec(), context.race())).thenReturn(10.0);

        WeaponItemPools itemPools = new WeaponItemPools(List.of(twoHand, currentWeapon), List.of(currentShield, currentWeapon));

        // when
        Map<Equipment.Slot, UpgradeResult> result = finder.findUpgrades(getHandSlotsEquippedPair(currentWeapon, currentShield),
                context, combos, itemPools);

        // then
        assertThat(result).containsOnlyKeys(MAIN_HAND, OFF_HAND);

        assertThat(result.get(MAIN_HAND)).isInstanceOfSatisfying(UpgradeResult.UpgradeFoundResult.class,
                res -> assertThat(res.item()).isEqualTo(twoHand));

        assertThat(result.get(OFF_HAND)).isInstanceOfSatisfying(UpgradeResult.SlotClearedResult.class,
                res -> assertThat(res.triggeringItem()).isEqualTo(twoHand));

        verify(powerCalculator, times(itemPools.mainHandPool().size() + itemPools.offHandPool().size()))
                .calculate(argThat(slot -> slot == MAIN_HAND || slot == OFF_HAND),
                        argThat((item) -> itemPools.mainHandPool().contains(item) || itemPools.offHandPool().contains(item)),
                        eq(context.classSpec()), eq(context.race()));
    }

    @Test
    void shouldSignalUpgradeFoundResults_TwoOneHandersBetterThanCurrentTwoHand() {
        // given
        CharacterContext context = new CharacterContext(ClassSpec.PALADIN_PROTECTION, WowCharacter.Race.HUMAN);
        HandSlotCombinations combos = HandSlotCombinations.from(new HandSlotTypes(List.of(TWO_HAND), List.of()),
                new HandSlotTypes(List.of(WEAPON, WEAPONMAINHAND), List.of(SHIELD)));

        Weapon currentTwoHand = createPowerfulTwoHandSword();
        Weapon oneHand = WeaponBuilder.newOneHandSword().build();
        Armor shield = ArmorBuilder.newShield().build();

        when(powerCalculator.calculate(MAIN_HAND, currentTwoHand, context.classSpec(), context.race())).thenReturn(70.0);
        when(powerCalculator.calculate(MAIN_HAND, oneHand, context.classSpec(), context.race())).thenReturn(50.0);
        when(powerCalculator.calculate(OFF_HAND, shield, context.classSpec(), context.race())).thenReturn(250.0);

        WeaponItemPools itemPools = new WeaponItemPools(List.of(currentTwoHand, oneHand), List.of(shield));

        // when
        Map<Equipment.Slot, UpgradeResult> result = finder.findUpgrades(getHandSlotsEquippedPair(currentTwoHand, null),
                context, combos, itemPools);

        // then
        assertThat(result).containsOnlyKeys(MAIN_HAND, OFF_HAND);

        assertThat(result.get(MAIN_HAND)).isInstanceOfSatisfying(UpgradeResult.UpgradeFoundResult.class,
                res -> assertThat(res.item()).isEqualTo(oneHand));

        assertThat(result.get(OFF_HAND)).isInstanceOfSatisfying(UpgradeResult.UpgradeFoundResult.class,
                res -> assertThat(res.item()).isEqualTo(shield));

        verify(powerCalculator, times(itemPools.mainHandPool().size() + itemPools.offHandPool().size()))
                .calculate(argThat(slot -> slot == MAIN_HAND || slot == OFF_HAND),
                        argThat((item) -> itemPools.mainHandPool().contains(item) || itemPools.offHandPool().contains(item)),
                        eq(context.classSpec()), eq(context.race()));
    }

    @Test
    void shouldSelectNextBestOffHandWeapon_CurrentBestWeaponUniqueEquipped() {
        // given
        CharacterContext context = defaultContext;

        Item powerfulUniqueWeapon = WeaponBuilder.newOneHandSword().withBaseId(1L).withUniqueEquipped(true).build();
        Item secondBestWeapon = WeaponBuilder.newOneHandSword().withBaseId(2L).build();
        Item otherWeapon = WeaponBuilder.newOneHandSword().withBaseId(3L).build();

        when(powerCalculator.calculate(MAIN_HAND, powerfulUniqueWeapon, context.classSpec(), context.race())).thenReturn(100.0);
        when(powerCalculator.calculate(MAIN_HAND, secondBestWeapon, context.classSpec(), context.race())).thenReturn(80.0);
        when(powerCalculator.calculate(MAIN_HAND, otherWeapon, context.classSpec(), context.race())).thenReturn(70.0);

        when(powerCalculator.calculate(OFF_HAND, powerfulUniqueWeapon, context.classSpec(), context.race())).thenReturn(50.0);
        when(powerCalculator.calculate(OFF_HAND, secondBestWeapon, context.classSpec(), context.race())).thenReturn(40.0);
        when(powerCalculator.calculate(OFF_HAND, otherWeapon, context.classSpec(), context.race())).thenReturn(35.0);

        List<Item> mainHandPool = List.of(powerfulUniqueWeapon, secondBestWeapon, otherWeapon);
        List<Item> offHandPool = List.of(powerfulUniqueWeapon, secondBestWeapon, otherWeapon);
        WeaponItemPools itemPools = new WeaponItemPools(mainHandPool, offHandPool);

        // when
        Map<Equipment.Slot, UpgradeResult> result = finder.findUpgrades(getHandSlotsEquippedPair(null, null),
                context, defaultCombos, itemPools);

        // then
        assertThat(result).containsOnlyKeys(MAIN_HAND, OFF_HAND);

        assertThat(result.get(MAIN_HAND)).isInstanceOfSatisfying(UpgradeResult.UpgradeFoundResult.class,
                res -> assertThat(res.item()).isEqualTo(powerfulUniqueWeapon));

        assertThat(result.get(OFF_HAND)).isInstanceOfSatisfying(UpgradeResult.UpgradeFoundResult.class,
                res -> assertThat(res.item()).isEqualTo(secondBestWeapon));

        verify(powerCalculator, times(mainHandPool.size() + offHandPool.size()))
                .calculate(argThat(slot -> slot == MAIN_HAND || slot == OFF_HAND),
                        argThat((item) -> mainHandPool.contains(item) || offHandPool.contains(item)),
                        eq(context.classSpec()), eq(context.race()));
    }

    @Test
    void shouldSignalBisAlreadyEquippedAndNoItemsAvailableResults_CurrentCasterTwoHandIsBetterThanTheOtherCombination() {
        // given
        CharacterContext context = new CharacterContext(ClassSpec.DRUID_BALANCE, WowCharacter.Race.NIGHT_ELF);
        HandSlotCombinations combos = HandSlotCombinations.from(
                new HandSlotTypes(List.of(TWO_HAND), List.of()),
                new HandSlotTypes(List.of(WEAPON, WEAPONMAINHAND), List.of(HOLDABLE))
        );

        Item bestStaff = createTwoHandStaff(1L, "Best Staff");
        Item otherStaff = createTwoHandStaff(2L, "Other Staff");
        Item oneHand = WeaponBuilder.newOneHandSword().withBaseId(3L).withName("Cool Mace").withInventoryType("WEAPON").withWeaponType("MACE").build();
        Item holdable = EquippableItemBuilder.newRing().withBaseId(4L).withName("Holdable").withInventoryType("HOLDABLE").build();

        when(powerCalculator.calculate(MAIN_HAND, bestStaff, context.classSpec(), context.race())).thenReturn(200.0);
        when(powerCalculator.calculate(MAIN_HAND, otherStaff, context.classSpec(), context.race())).thenReturn(140.0);
        when(powerCalculator.calculate(MAIN_HAND, oneHand, context.classSpec(), context.race())).thenReturn(110.0);
        when(powerCalculator.calculate(OFF_HAND, holdable, context.classSpec(), context.race())).thenReturn(90.0);

        List<Item> mainHandPool = List.of(bestStaff, otherStaff, oneHand);
        List<Item> offHandPool = List.of(holdable);
        WeaponItemPools itemPools = new WeaponItemPools(mainHandPool, offHandPool);

        // when
        Map<Equipment.Slot, UpgradeResult> result = finder.findUpgrades(getHandSlotsEquippedPair(bestStaff, null),
                context, combos, itemPools);

        // then
        assertThat(result).containsOnlyKeys(MAIN_HAND, OFF_HAND);

        assertThat(result.get(MAIN_HAND)).isInstanceOfSatisfying(UpgradeResult.AlreadyBiSResult.class,
                res -> assertThat(res.item()).isEqualTo(bestStaff));

        assertThat(result.get(OFF_HAND)).isInstanceOf(UpgradeResult.NoItemsAvailableResult.class);

        verify(powerCalculator, times(mainHandPool.size() + offHandPool.size()))
                .calculate(argThat(slot -> slot == MAIN_HAND || slot == OFF_HAND),
                        argThat((item) -> mainHandPool.contains(item) || offHandPool.contains(item)),
                        eq(context.classSpec()), eq(context.race()));
    }

    @Test
    void shouldSignalBisAlreadyEquippedForBothSlots_BiSCombinationAlreadyWorn() {
        // given
        CharacterContext context = new CharacterContext(ClassSpec.WARRIOR_PROTECTION, WowCharacter.Race.HUMAN);
        HandSlotCombinations combos = HandSlotCombinations.from(
                new HandSlotTypes(List.of(TWO_HAND), List.of()),
                new HandSlotTypes(List.of(WEAPON, WEAPONMAINHAND), List.of(WEAPON, WEAPONOFFHAND, SHIELD))
        );

        Item bestOneHand = WeaponBuilder.newOneHandSword().withBaseId(1L).withName("Best Sword").withInventoryType("WEAPON").build();
        Item otherOneHand = WeaponBuilder.newOneHandSword().withBaseId(2L).withName("Best Sword").withInventoryType("WEAPON").build();
        Item someTwoHand = createPowerfulTwoHandSword();
        Armor bestShield = ArmorBuilder.newShield().withBaseId(3L).build();
        Armor otherShield = ArmorBuilder.newShield().withBaseId(4L).build();

        when(powerCalculator.calculate(MAIN_HAND, bestOneHand, context.classSpec(), context.race())).thenReturn(60.0);
        when(powerCalculator.calculate(OFF_HAND, bestOneHand, context.classSpec(), context.race())).thenReturn(10.0);
        when(powerCalculator.calculate(MAIN_HAND, otherOneHand, context.classSpec(), context.race())).thenReturn(50.0);
        when(powerCalculator.calculate(OFF_HAND, otherOneHand, context.classSpec(), context.race())).thenReturn(5.0);
        when(powerCalculator.calculate(MAIN_HAND, someTwoHand, context.classSpec(), context.race())).thenReturn(60.0);
        when(powerCalculator.calculate(OFF_HAND, bestShield, context.classSpec(), context.race())).thenReturn(90.0);
        when(powerCalculator.calculate(OFF_HAND, otherShield, context.classSpec(), context.race())).thenReturn(80.0);

        List<Item> mainHandPool = List.of(bestOneHand, otherOneHand, someTwoHand);
        List<Item> offHandPool = List.of(bestShield, otherShield, bestOneHand, otherOneHand);
        WeaponItemPools itemPools = new WeaponItemPools(mainHandPool, offHandPool);

        // when
        Map<Equipment.Slot, UpgradeResult> result = finder.findUpgrades(getHandSlotsEquippedPair(bestOneHand, bestShield),
                context, combos, itemPools);

        // then
        assertThat(result).containsOnlyKeys(MAIN_HAND, OFF_HAND);

        assertThat(result.get(MAIN_HAND)).isInstanceOfSatisfying(UpgradeResult.AlreadyBiSResult.class,
                res -> assertThat(res.item()).isEqualTo(bestOneHand));

        assertThat(result.get(OFF_HAND)).isInstanceOfSatisfying(UpgradeResult.AlreadyBiSResult.class,
                res -> assertThat(res.item()).isEqualTo(bestShield));

        verify(powerCalculator, times(mainHandPool.size() + offHandPool.size()))
                .calculate(argThat(slot -> slot == MAIN_HAND || slot == OFF_HAND),
                        argThat((item) -> mainHandPool.contains(item) || offHandPool.contains(item)),
                        eq(context.classSpec()), eq(context.race()));
    }

    @Test
    void shouldPrioritizeComboWithEquippedMatchesOnScoreTie() {
        // given
        CharacterContext context = defaultContext;

        Item equippedMain = WeaponBuilder.newOneHandSword().withBaseId(1L).withName("Equipped MH").build();
        Item equippedOff = WeaponBuilder.newOneHandSword().withBaseId(2L).withName("Equipped OH").build();

        Item alternativeMain = WeaponBuilder.newOneHandSword().withBaseId(3L).withName("Alt MH").build();
        Item alternativeOff = WeaponBuilder.newOneHandSword().withBaseId(4L).withName("Alt OH").build();

        // Both combinations yield the exact same score (100.0)
        when(powerCalculator.calculate(MAIN_HAND, equippedMain, context.classSpec(), context.race())).thenReturn(50.0);
        when(powerCalculator.calculate(OFF_HAND, equippedOff, context.classSpec(), context.race())).thenReturn(50.0);
        when(powerCalculator.calculate(MAIN_HAND, alternativeMain, context.classSpec(), context.race())).thenReturn(50.0);
        when(powerCalculator.calculate(OFF_HAND, alternativeOff, context.classSpec(), context.race())).thenReturn(50.0);

        List<Item> mainPool = List.of(equippedMain, alternativeMain);
        List<Item> offPool = List.of(equippedOff, alternativeOff);
        WeaponItemPools itemPools = new WeaponItemPools(mainPool, offPool);

        // when
        Map<Equipment.Slot, UpgradeResult> result = finder.findUpgrades(getHandSlotsEquippedPair(equippedMain, equippedOff),
                context, defaultCombos, itemPools);

        // then - should prefer the combo containing the already equipped items due to tie-breaker
        assertThat(result).containsOnlyKeys(MAIN_HAND, OFF_HAND);

        assertThat(result.get(MAIN_HAND)).isInstanceOfSatisfying(UpgradeResult.AlreadyBiSResult.class,
                res -> assertThat(res.item()).isEqualTo(equippedMain));

        assertThat(result.get(OFF_HAND)).isInstanceOfSatisfying(UpgradeResult.AlreadyBiSResult.class,
                res -> assertThat(res.item()).isEqualTo(equippedOff));
    }

    @Test
    void shouldSignalUpgradesForBothSlotsWhenTransitioningFromTwoHandToOneHandPlusShieldIsBetter() {
        // given
        CharacterContext context = new CharacterContext(ClassSpec.WARRIOR_NONE, WowCharacter.Race.ORC);
        HandSlotCombinations combos = HandSlotCombinations.from(
                new HandSlotTypes(List.of(TWO_HAND), List.of()),
                new HandSlotTypes(List.of(WEAPON, WEAPONMAINHAND), List.of(SHIELD)) // No off-hand allowed/available
        );

        Weapon currentTwoHand = createPowerfulTwoHandSword();
        Weapon upgradeOneHand = WeaponBuilder.newOneHandSword().withBaseId(99L).withName("Better 1H").build();
        Armor shield = ArmorBuilder.newShield().withBaseId(3L).build();

        when(powerCalculator.calculate(MAIN_HAND, currentTwoHand, context.classSpec(), context.race())).thenReturn(70.0);
        when(powerCalculator.calculate(MAIN_HAND, upgradeOneHand, context.classSpec(), context.race())).thenReturn(60.0);
        when(powerCalculator.calculate(OFF_HAND, shield, context.classSpec(), context.race())).thenReturn(20.0);

        WeaponItemPools itemPools = new WeaponItemPools(List.of(currentTwoHand, upgradeOneHand), List.of(shield));

        // when
        Map<Equipment.Slot, UpgradeResult> result = finder.findUpgrades(getHandSlotsEquippedPair(currentTwoHand, null),
                context, combos, itemPools);

        // then
        assertThat(result).containsOnlyKeys(MAIN_HAND, OFF_HAND);

        assertThat(result.get(MAIN_HAND)).isInstanceOfSatisfying(UpgradeResult.UpgradeFoundResult.class,
                res -> assertThat(res.item()).isEqualTo(upgradeOneHand));

        assertThat(result.get(OFF_HAND)).isInstanceOfSatisfying(UpgradeResult.UpgradeFoundResult.class,
                res -> assertThat(res.item()).isEqualTo(shield));
    }

    private EquippedItemPair getHandSlotsEquippedPair(Item mainHand, Item offHand) {
        return new EquippedItemPair(
                new EquippedItemPair.Item(MAIN_HAND, mainHand != null ? Optional.of(new EquippedItem(mainHand, List.of())) : Optional.empty()),
                new EquippedItemPair.Item(OFF_HAND, offHand != null ? Optional.of(new EquippedItem(offHand, List.of())) : Optional.empty())
        );
    }

    private Weapon createPowerfulTwoHandSword() {
        return WeaponBuilder.newOneHandSword()
                .withBaseId(19854L)
                .withName("Zin'rokh, Destroyer of Worlds")
                .withInventoryType("TWO_HAND")
                .withWeaponType("SWORD")
                .withStats(Map.of("STAMINA", 28, "ATTACK_POWER", 72))
                .withMinDamage(196)
                .withMaxDamage(295)
                .withDps(64.61)
                .withSpeed(3.80)
                .build();
    }

    private Weapon createTwoHandStaff(long baseId, String name) {
        return WeaponBuilder.newOneHandSword()
                .withBaseId(baseId)
                .withName(name)
                .withInventoryType("TWO_HAND")
                .withWeaponType("STAFF")
                .build();
    }
}
