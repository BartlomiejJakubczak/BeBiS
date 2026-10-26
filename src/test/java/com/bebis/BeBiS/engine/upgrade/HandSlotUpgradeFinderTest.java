package com.bebis.BeBiS.engine.upgrade;

import com.bebis.BeBiS.engine.upgrade.domain.EquippedItemPair;
import com.bebis.BeBiS.engine.upgrade.domain.HandSlotCombinations;
import com.bebis.BeBiS.engine.upgrade.domain.HandSlotTypes;
import com.bebis.BeBiS.engine.upgrade.domain.ItemPool;
import com.bebis.BeBiS.engine.upgrade.domain.ScoredItem;
import com.bebis.BeBiS.engine.upgrade.domain.ScoredItemPool;
import com.bebis.BeBiS.engine.upgrade.domain.UpgradeResult;
import com.bebis.BeBiS.equipment.domain.Equipment;
import com.bebis.BeBiS.equipment.domain.EquippedItem;
import com.bebis.BeBiS.item.ArmorBuilder;
import com.bebis.BeBiS.item.EquippableItemBuilder;
import com.bebis.BeBiS.item.WeaponBuilder;
import com.bebis.BeBiS.item.domain.Armor;
import com.bebis.BeBiS.item.domain.Item;
import com.bebis.BeBiS.item.domain.QuestSource;
import com.bebis.BeBiS.item.domain.RewardKind;
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
import java.util.Set;

import static com.bebis.BeBiS.engine.upgrade.HandSlotUpgradeOrchestrator.CharacterContext;
import static com.bebis.BeBiS.engine.upgrade.HandSlotUpgradeOrchestrator.HandSlotItemPools;
import static com.bebis.BeBiS.equipment.domain.Equipment.Slot.MAIN_HAND;
import static com.bebis.BeBiS.equipment.domain.Equipment.Slot.OFF_HAND;
import static com.bebis.BeBiS.item.domain.Item.InventoryType.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class HandSlotUpgradeFinderTest {

    @Mock
    private ItemScorer scorer;

    private HandSlotUpgradeFinder finder;

    // test data
    private final CharacterContext defaultContext = new CharacterContext(ClassSpec.ROGUE_COMBAT, WowCharacter.Race.NIGHT_ELF);
    private final HandSlotCombinations defaultCombos = HandSlotCombinations.from(new HandSlotTypes(List.of(WEAPON, WEAPONMAINHAND), List.of(WEAPON, WEAPONOFFHAND)));

    @BeforeEach
    void setUp() {
        finder = new HandSlotUpgradeFinder(scorer, new UpgradeResultEvaluator());
    }

    @Test
    void shouldSignalNoItemsAvailableResult_EmptyItemPools() {
        // given / when
        Map<Equipment.Slot, UpgradeResult> result = finder.findUpgrades(getHandSlotsEquippedPair(null, null),
                defaultContext, defaultCombos, new HandSlotItemPools(
                        givenPoolScoredAs(MAIN_HAND, defaultContext, Map.of()),
                        givenPoolScoredAs(OFF_HAND, defaultContext, Map.of())));
        // then
        assertThat(result).containsOnlyKeys(MAIN_HAND, OFF_HAND);
        assertThat(result.values()).hasOnlyElementsOfType(UpgradeResult.NoItemsAvailableResult.class);
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

        HandSlotItemPools itemPools = new HandSlotItemPools(
                givenPoolScoredAs(MAIN_HAND, context, Map.of(twoHand, 250.0, currentWeapon, 50.0)),
                givenPoolScoredAs(OFF_HAND, context, Map.of(currentWeapon, 25.0, currentShield, 10.0)));

        // when
        Map<Equipment.Slot, UpgradeResult> result = finder.findUpgrades(getHandSlotsEquippedPair(currentWeapon, currentShield),
                context, combos, itemPools);

        // then
        assertThat(result).containsOnlyKeys(MAIN_HAND, OFF_HAND);

        assertThat(result.get(MAIN_HAND)).isInstanceOfSatisfying(UpgradeResult.UpgradeFoundResult.class,
                res -> assertThat(res.item()).isEqualTo(twoHand));

        assertThat(result.get(OFF_HAND)).isInstanceOfSatisfying(UpgradeResult.SlotClearedResult.class,
                res -> assertThat(res.triggeringItem()).isEqualTo(twoHand));
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

        HandSlotItemPools itemPools = new HandSlotItemPools(
                givenPoolScoredAs(MAIN_HAND, context, Map.of(currentTwoHand, 70.0, oneHand, 50.0)),
                givenPoolScoredAs(OFF_HAND, context, Map.of(shield, 250.0)));

        // when
        Map<Equipment.Slot, UpgradeResult> result = finder.findUpgrades(getHandSlotsEquippedPair(currentTwoHand, null),
                context, combos, itemPools);

        // then
        assertThat(result).containsOnlyKeys(MAIN_HAND, OFF_HAND);

        assertThat(result.get(MAIN_HAND)).isInstanceOfSatisfying(UpgradeResult.UpgradeFoundResult.class,
                res -> assertThat(res.item()).isEqualTo(oneHand));

        assertThat(result.get(OFF_HAND)).isInstanceOfSatisfying(UpgradeResult.UpgradeFoundResult.class,
                res -> assertThat(res.item()).isEqualTo(shield));
    }

    @Test
    void shouldSelectNextBestOffHandWeapon_CurrentBestWeaponUniqueEquipped() {
        // given
        CharacterContext context = defaultContext;

        Item powerfulUniqueWeapon = WeaponBuilder.newOneHandSword().withBaseId(1L).withUniqueEquipped(true).build();
        Item secondBestWeapon = WeaponBuilder.newOneHandSword().withBaseId(2L).build();
        Item otherWeapon = WeaponBuilder.newOneHandSword().withBaseId(3L).build();

        HandSlotItemPools itemPools = new HandSlotItemPools(
                givenPoolScoredAs(MAIN_HAND, context, Map.of(powerfulUniqueWeapon, 100.0, secondBestWeapon, 80.0, otherWeapon, 70.0)),
                givenPoolScoredAs(OFF_HAND, context, Map.of(powerfulUniqueWeapon, 50.0, secondBestWeapon, 40.0, otherWeapon, 35.0)));

        // when
        Map<Equipment.Slot, UpgradeResult> result = finder.findUpgrades(getHandSlotsEquippedPair(null, null),
                context, defaultCombos, itemPools);

        // then
        assertThat(result).containsOnlyKeys(MAIN_HAND, OFF_HAND);

        assertThat(result.get(MAIN_HAND)).isInstanceOfSatisfying(UpgradeResult.UpgradeFoundResult.class,
                res -> assertThat(res.item()).isEqualTo(powerfulUniqueWeapon));

        assertThat(result.get(OFF_HAND)).isInstanceOfSatisfying(UpgradeResult.UpgradeFoundResult.class,
                res -> assertThat(res.item()).isEqualTo(secondBestWeapon));
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

        HandSlotItemPools itemPools = new HandSlotItemPools(
                givenPoolScoredAs(MAIN_HAND, context, Map.of(bestStaff, 200.0, otherStaff, 140.0, oneHand, 110.0)),
                givenPoolScoredAs(OFF_HAND, context, Map.of(holdable, 90.0)));

        // when
        Map<Equipment.Slot, UpgradeResult> result = finder.findUpgrades(getHandSlotsEquippedPair(bestStaff, null),
                context, combos, itemPools);

        // then
        assertThat(result).containsOnlyKeys(MAIN_HAND, OFF_HAND);

        assertThat(result.get(MAIN_HAND)).isInstanceOfSatisfying(UpgradeResult.AlreadyBiSResult.class,
                res -> assertThat(res.item()).isEqualTo(bestStaff));

        assertThat(result.get(OFF_HAND)).isInstanceOf(UpgradeResult.NoItemsAvailableResult.class);
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

        HandSlotItemPools itemPools = new HandSlotItemPools(
                givenPoolScoredAs(MAIN_HAND, context, Map.of(bestOneHand, 60.0, otherOneHand, 50.0, someTwoHand, 60.0)),
                givenPoolScoredAs(OFF_HAND, context, Map.of(bestOneHand, 10.0, otherOneHand, 5.0, bestShield, 90.0, otherShield, 80.0)));

        // when
        Map<Equipment.Slot, UpgradeResult> result = finder.findUpgrades(getHandSlotsEquippedPair(bestOneHand, bestShield),
                context, combos, itemPools);

        // then
        assertThat(result).containsOnlyKeys(MAIN_HAND, OFF_HAND);

        assertThat(result.get(MAIN_HAND)).isInstanceOfSatisfying(UpgradeResult.AlreadyBiSResult.class,
                res -> assertThat(res.item()).isEqualTo(bestOneHand));

        assertThat(result.get(OFF_HAND)).isInstanceOfSatisfying(UpgradeResult.AlreadyBiSResult.class,
                res -> assertThat(res.item()).isEqualTo(bestShield));
    }

    @Test
    void shouldPrioritizeComboWithEquippedMatchesOnScoreTie() {
        // given
        CharacterContext context = defaultContext;

        Item equippedMain = WeaponBuilder.newOneHandSword().withBaseId(1L).withName("Equipped MH").build();
        Item equippedOff = WeaponBuilder.newOneHandSword().withBaseId(2L).withName("Equipped OH").build();

        Item alternativeMain = WeaponBuilder.newOneHandSword().withBaseId(3L).withName("Alt MH").build();
        Item alternativeOff = WeaponBuilder.newOneHandSword().withBaseId(4L).withName("Alt OH").build();

        HandSlotItemPools itemPools = new HandSlotItemPools(
                givenPoolScoredAs(MAIN_HAND, context, Map.of(equippedMain, 50.0, alternativeMain, 50.0)),
                givenPoolScoredAs(OFF_HAND, context, Map.of(equippedOff, 50.0, alternativeOff, 50.0)));

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

        HandSlotItemPools itemPools = new HandSlotItemPools(
                givenPoolScoredAs(MAIN_HAND, context, Map.of(currentTwoHand, 70.0, upgradeOneHand, 60.0)),
                givenPoolScoredAs(OFF_HAND, context, Map.of(shield, 20.0)));

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

    @Test
    void shouldSelectNextBestOffHandWeapon_BestWeaponIsQuestReward() {
        // given (Outlaw Sabre: a one-handed quest reward that isn't Unique, so only its quest source keeps it to one hand)
        CharacterContext context = new CharacterContext(ClassSpec.ROGUE_COMBAT, WowCharacter.Race.ORC);

        Item outlawSabre = WeaponBuilder.newOneHandSword().withBaseId(16886L).withName("Outlaw Sabre")
                .withUniqueEquipped(false)
                .withSources(new QuestSource(909L, 30, 25, RewardKind.CHOICE, Set.of(),
                        Set.of(WowCharacter.Race.ORC, WowCharacter.Race.UNDEAD, WowCharacter.Race.TAUREN, WowCharacter.Race.TROLL)))
                .build();
        Item secondBestWeapon = WeaponBuilder.newOneHandSword().withBaseId(2L).build();

        HandSlotItemPools itemPools = new HandSlotItemPools(
                givenPoolScoredAs(MAIN_HAND, context, Map.of(outlawSabre, 100.0, secondBestWeapon, 80.0)),
                givenPoolScoredAs(OFF_HAND, context, Map.of(outlawSabre, 50.0, secondBestWeapon, 40.0)));

        // when
        Map<Equipment.Slot, UpgradeResult> result = finder.findUpgrades(getHandSlotsEquippedPair(null, null),
                context, defaultCombos, itemPools);

        // then
        assertThat(result.get(MAIN_HAND)).isInstanceOfSatisfying(UpgradeResult.UpgradeFoundResult.class,
                res -> assertThat(res.item()).isEqualTo(outlawSabre));
        assertThat(result.get(OFF_HAND)).isInstanceOfSatisfying(UpgradeResult.UpgradeFoundResult.class,
                res -> assertThat(res.item()).isEqualTo(secondBestWeapon));
    }

    /**
     * Stubs the scorer to score the returned pool with the given scores for this hand. The same weapon usually scores
     * differently in each hand.
     */
    private ItemPool givenPoolScoredAs(Equipment.Slot slot, CharacterContext context, Map<? extends Item, Double> scores) {
        ItemPool itemPool = new ItemPool(List.copyOf(scores.keySet()));
        List<ScoredItem> scoredItems = scores.entrySet().stream()
                .map(entry -> new ScoredItem(entry.getKey(), entry.getValue()))
                .toList();
        when(scorer.scoreItemPool(slot, itemPool, context.classSpec(), context.race())).thenReturn(new ScoredItemPool(scoredItems));
        return itemPool;
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
