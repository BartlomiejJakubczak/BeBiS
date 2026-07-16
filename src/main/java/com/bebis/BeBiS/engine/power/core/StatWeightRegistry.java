package com.bebis.BeBiS.engine.power.core;

import com.bebis.BeBiS.engine.power.domain.StatWeights;
import com.bebis.BeBiS.item.domain.StatType;
import com.bebis.BeBiS.profile.domain.ClassSpec;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Component
class StatWeightRegistry {

    private final Map<ClassSpec, StatWeights> registry = new EnumMap<>(ClassSpec.class);

    public StatWeightRegistry() {
        populateRegistry();
    }

    public StatWeights getWeightsFor(ClassSpec classSpec) {
        return registry.getOrDefault(classSpec, new StatWeights(new EnumMap<>(StatType.class)));
    }

    private void populateRegistry() {
        // ==========================================
        // BASELINE CLASSES (Level 1-9 or unspent talents)
        // Balanced around raw leveling efficiency/momentum.
        // ==========================================
        Map<StatType, Double> warriorNone = new EnumMap<>(StatType.class);
        warriorNone.put(StatType.STRENGTH, 1.0);
        warriorNone.put(StatType.AGILITY, 0.6);
        warriorNone.put(StatType.STAMINA, 0.4);
        warriorNone.put(StatType.ARMOR, 0.01);
        registry.put(ClassSpec.WARRIOR_NONE, new StatWeights(warriorNone));

        Map<StatType, Double> rogueNone = new EnumMap<>(StatType.class);
        rogueNone.put(StatType.AGILITY, 1.0);
        rogueNone.put(StatType.STRENGTH, 0.6);
        rogueNone.put(StatType.STAMINA, 0.4);
        rogueNone.put(StatType.ARMOR, 0.01);
        registry.put(ClassSpec.ROGUE_NONE, new StatWeights(rogueNone));

        Map<StatType, Double> hunterNone = new EnumMap<>(StatType.class);
        hunterNone.put(StatType.AGILITY, 1.0);
        hunterNone.put(StatType.INTELLECT, 0.4);
        hunterNone.put(StatType.STAMINA, 0.3);
        hunterNone.put(StatType.ARMOR, 0.01);
        registry.put(ClassSpec.HUNTER_NONE, new StatWeights(hunterNone));

        Map<StatType, Double> mageNone = new EnumMap<>(StatType.class);
        mageNone.put(StatType.INTELLECT, 1.0);
        mageNone.put(StatType.SPIRIT, 0.7);
        mageNone.put(StatType.STAMINA, 0.3);
        mageNone.put(StatType.ARMOR, 0.001);
        registry.put(ClassSpec.MAGE_NONE, new StatWeights(mageNone));

        Map<StatType, Double> warlockNone = new EnumMap<>(StatType.class);
        warlockNone.put(StatType.INTELLECT, 1.0);
        warlockNone.put(StatType.STAMINA, 0.7); // High value due to Life Tap pacing
        warlockNone.put(StatType.SPIRIT, 0.4);
        warlockNone.put(StatType.ARMOR, 0.001);
        registry.put(ClassSpec.WARLOCK_NONE, new StatWeights(warlockNone));

        Map<StatType, Double> priestNone = new EnumMap<>(StatType.class);
        priestNone.put(StatType.INTELLECT, 1.0);
        priestNone.put(StatType.SPIRIT, 0.8); // High value due to raw leveling downtime
        priestNone.put(StatType.STAMINA, 0.3);
        priestNone.put(StatType.ARMOR, 0.001);
        registry.put(ClassSpec.PRIEST_NONE, new StatWeights(priestNone));

        Map<StatType, Double> paladinNone = new EnumMap<>(StatType.class);
        paladinNone.put(StatType.STRENGTH, 1.0);
        paladinNone.put(StatType.STAMINA, 0.6);
        paladinNone.put(StatType.INTELLECT, 0.5);
        paladinNone.put(StatType.SPIRIT, 0.4);
        paladinNone.put(StatType.ARMOR, 0.01);
        registry.put(ClassSpec.PALADIN_NONE, new StatWeights(paladinNone));

        Map<StatType, Double> shamanNone = new EnumMap<>(StatType.class);
        shamanNone.put(StatType.STRENGTH, 1.0);
        shamanNone.put(StatType.STAMINA, 0.6);
        shamanNone.put(StatType.INTELLECT, 0.5);
        shamanNone.put(StatType.SPIRIT, 0.4);
        shamanNone.put(StatType.ARMOR, 0.01);
        registry.put(ClassSpec.SHAMAN_NONE, new StatWeights(shamanNone));

        Map<StatType, Double> druidNone = new EnumMap<>(StatType.class);
        druidNone.put(StatType.STRENGTH, 1.0);
        druidNone.put(StatType.STAMINA, 0.6);
        druidNone.put(StatType.INTELLECT, 0.5);
        druidNone.put(StatType.SPIRIT, 0.4);
        druidNone.put(StatType.ARMOR, 0.01);
        registry.put(ClassSpec.DRUID_NONE, new StatWeights(druidNone));

        // ==========================================
        // WARRIOR
        // ==========================================
        Map<StatType, Double> armsWarrior = new EnumMap<>(StatType.class);
        armsWarrior.put(StatType.STRENGTH, 2.0);
        armsWarrior.put(StatType.ATTACK_POWER, 1.0);
        armsWarrior.put(StatType.AGILITY, 1.0);
        armsWarrior.put(StatType.CRIT_RATING, 25.0);
        armsWarrior.put(StatType.HIT_RATING, 21.0);
        armsWarrior.put(StatType.STAMINA, 0.2);
        armsWarrior.put(StatType.ARMOR, 0.01);
        armsWarrior.put(StatType.WEAPON_DPS, 14.0); // High physical scaling
        armsWarrior.put(StatType.WEAPON_SPEED, 25.0); // Heavily demands slow weapons for Mortal Strike
        registry.put(ClassSpec.WARRIOR_ARMS, new StatWeights(armsWarrior));

        Map<StatType, Double> furyWarrior = new EnumMap<>(StatType.class);
        furyWarrior.put(StatType.STRENGTH, 2.0);
        furyWarrior.put(StatType.ATTACK_POWER, 1.0);
        furyWarrior.put(StatType.AGILITY, 1.0);
        furyWarrior.put(StatType.CRIT_RATING, 25.0);
        furyWarrior.put(StatType.HIT_RATING, 21.0);
        furyWarrior.put(StatType.STAMINA, 0.1);
        furyWarrior.put(StatType.ARMOR, 0.01);
        furyWarrior.put(StatType.WEAPON_DPS, 16.5); // Dual-wield white damage scales boundlessly with raw DPS
        furyWarrior.put(StatType.WEAPON_SPEED, 5.0);  // Mildly favors slow main-hand, but pure DPS dominates
        registry.put(ClassSpec.WARRIOR_FURY, new StatWeights(furyWarrior));

        Map<StatType, Double> protWarrior = new EnumMap<>(StatType.class);
        protWarrior.put(StatType.STAMINA, 1.5);
        protWarrior.put(StatType.DEFENSE_RATING, 2.0);
        protWarrior.put(StatType.STRENGTH, 1.2);
        protWarrior.put(StatType.ATTACK_POWER, 0.5);
        protWarrior.put(StatType.DODGE_RATING, 1.2);
        protWarrior.put(StatType.PARRY_RATING, 1.4);
        protWarrior.put(StatType.BLOCK_CHANCE, 1.0);
        protWarrior.put(StatType.BLOCK_VALUE, 1.5);
        protWarrior.put(StatType.AGILITY, 0.8);
        protWarrior.put(StatType.ARMOR, 0.04);
        protWarrior.put(StatType.HIT_RATING, 15.0);
        protWarrior.put(StatType.CRIT_RATING, 10.0);
        protWarrior.put(StatType.WEAPON_DPS, 9.0); // Needed for threat generation
        protWarrior.put(StatType.WEAPON_SPEED, -5.0); // Negative value! Deep prot favors fast weapons for Heroic Strike queueing
        registry.put(ClassSpec.WARRIOR_PROTECTION, new StatWeights(protWarrior));

        // ==========================================
        // ROGUE
        // ==========================================
        Map<StatType, Double> assRogue = new EnumMap<>(StatType.class);
        assRogue.put(StatType.AGILITY, 1.6);
        assRogue.put(StatType.ATTACK_POWER, 1.0);
        assRogue.put(StatType.STRENGTH, 0.6);
        assRogue.put(StatType.ARMOR, 0.01);
        assRogue.put(StatType.CRIT_RATING, 23.0);
        assRogue.put(StatType.HIT_RATING, 19.0);
        assRogue.put(StatType.WEAPON_DPS, 12.0);
        assRogue.put(StatType.WEAPON_SPEED, -10.0); // Daggers only, favors faster offhands for poison procs
        registry.put(ClassSpec.ROGUE_ASSASSINATION, new StatWeights(assRogue));

        Map<StatType, Double> combatRogue = new EnumMap<>(StatType.class);
        combatRogue.put(StatType.AGILITY, 1.5);
        combatRogue.put(StatType.ATTACK_POWER, 1.0);
        combatRogue.put(StatType.STRENGTH, 1.0);
        combatRogue.put(StatType.ARMOR, 0.01);
        combatRogue.put(StatType.CRIT_RATING, 24.0);
        combatRogue.put(StatType.HIT_RATING, 18.0);
        combatRogue.put(StatType.WEAPON_DPS, 15.0);  // Massive sword/mace scaling
        combatRogue.put(StatType.WEAPON_SPEED, 12.0);  // Favors hard-hitting slow main-hands for Sinister Strike
        registry.put(ClassSpec.ROGUE_COMBAT, new StatWeights(combatRogue));

        Map<StatType, Double> subRogue = new EnumMap<>(StatType.class);
        subRogue.put(StatType.AGILITY, 1.4);
        subRogue.put(StatType.ATTACK_POWER, 1.0);
        subRogue.put(StatType.STRENGTH, 0.8);
        subRogue.put(StatType.ARMOR, 0.01);
        subRogue.put(StatType.CRIT_RATING, 22.0);
        subRogue.put(StatType.HIT_RATING, 17.0);
        subRogue.put(StatType.WEAPON_DPS, 11.0);
        subRogue.put(StatType.WEAPON_SPEED, 15.0);  // Ambush/Backstab scaling heavily favors slow daggers
        registry.put(ClassSpec.ROGUE_SUBTLETY, new StatWeights(subRogue));

        // ==========================================
        // PALADIN
        // ==========================================
        Map<StatType, Double> holyPala = new EnumMap<>(StatType.class);
        holyPala.put(StatType.HEALING_POWER, 1.0);
        holyPala.put(StatType.INTELLECT, 1.0);
        holyPala.put(StatType.SPELL_POWER, 0.85); // Grants damage + healing
        holyPala.put(StatType.MANA_PER_5_SECONDS, 5.0);
        holyPala.put(StatType.SPELL_CRIT_RATING, 7.0);
        holyPala.put(StatType.SPIRIT, 0.1); // Spellcasting stops mana regen for 5 seconds, pretty much a dead stat for holy palas
        holyPala.put(StatType.STAMINA, 0.2);
        holyPala.put(StatType.ARMOR, 0.001);
        // Melee stats omitted (Pure caster stick)
        registry.put(ClassSpec.PALADIN_HOLY, new StatWeights(holyPala));

        Map<StatType, Double> protPala = new EnumMap<>(StatType.class);
        protPala.put(StatType.STAMINA, 1.5);
        protPala.put(StatType.SPELL_POWER, 1.2); // Main source of Consecration threat
        protPala.put(StatType.DEFENSE_RATING, 1.5);
        protPala.put(StatType.DODGE_RATING, 1.0);
        protPala.put(StatType.PARRY_RATING, 1.2);
        protPala.put(StatType.BLOCK_CHANCE, 1.0);
        protPala.put(StatType.BLOCK_VALUE, 1.0);
        protPala.put(StatType.STRENGTH, 1.0);
        protPala.put(StatType.INTELLECT, 0.5);
        protPala.put(StatType.MANA_PER_5_SECONDS, 2.0); // Tanks that rely on mana
        protPala.put(StatType.ARMOR, 0.04);
        protPala.put(StatType.HIT_RATING, 12.0);
        protPala.put(StatType.WEAPON_DPS, 6.0); // Minor scaling, mostly spell power threat
        registry.put(ClassSpec.PALADIN_PROTECTION, new StatWeights(protPala));

        Map<StatType, Double> retPala = new EnumMap<>(StatType.class);
        retPala.put(StatType.STRENGTH, 2.0);
        retPala.put(StatType.ARMOR, 0.01);
        retPala.put(StatType.ATTACK_POWER, 1.0);
        retPala.put(StatType.SPELL_POWER, 0.6); // Spells scale with Judgements/Consecration
        retPala.put(StatType.AGILITY, 1.0);
        retPala.put(StatType.CRIT_RATING, 22.0);
        retPala.put(StatType.HIT_RATING, 20.0);
        retPala.put(StatType.INTELLECT, 0.5);
        retPala.put(StatType.MANA_PER_5_SECONDS, 1.5);
        retPala.put(StatType.WEAPON_DPS, 11.0);
        retPala.put(StatType.WEAPON_SPEED, 35.0); // Extreme premium on slow speed due to Seal of Command PPM mechanics
        registry.put(ClassSpec.PALADIN_RETRIBUTION, new StatWeights(retPala));

        // ==========================================
        // HUNTER
        // ==========================================
        Map<StatType, Double> bmHunter = new EnumMap<>(StatType.class);
        bmHunter.put(StatType.RANGED_ATTACK_POWER, 1.0);
        bmHunter.put(StatType.AGILITY, 2.0);
        bmHunter.put(StatType.CRIT_RATING, 24.0);
        bmHunter.put(StatType.HIT_RATING, 22.0);
        bmHunter.put(StatType.MANA_PER_5_SECONDS, 1.0);
        bmHunter.put(StatType.STAMINA, 0.2);
        bmHunter.put(StatType.ARMOR, 0.01);
        bmHunter.put(StatType.WEAPON_DPS, 20.0); // Applies to Ranged Weapons
        bmHunter.put(StatType.WEAPON_SPEED, 5.0);
        registry.put(ClassSpec.HUNTER_BEASTMASTERY, new StatWeights(bmHunter));

        Map<StatType, Double> mmHunter = new EnumMap<>(StatType.class);
        mmHunter.put(StatType.RANGED_ATTACK_POWER, 1.0);
        mmHunter.put(StatType.AGILITY, 2.5);
        mmHunter.put(StatType.CRIT_RATING, 26.0);
        mmHunter.put(StatType.HIT_RATING, 24.0);
        mmHunter.put(StatType.MANA_PER_5_SECONDS, 1.0);
        mmHunter.put(StatType.STAMINA, 0.2);
        mmHunter.put(StatType.ARMOR, 0.01);
        mmHunter.put(StatType.WEAPON_DPS, 28.0); // Ranged DPS is an astronomical upgrade for MM
        mmHunter.put(StatType.WEAPON_SPEED, 18.0); // High values favor slow weapons to prevent clipping Aimed Strike
        registry.put(ClassSpec.HUNTER_MARKSMANSHIP, new StatWeights(mmHunter));

        Map<StatType, Double> survHunter = new EnumMap<>(StatType.class);
        survHunter.put(StatType.RANGED_ATTACK_POWER, 1.0);
        survHunter.put(StatType.AGILITY, 2.2);
        survHunter.put(StatType.CRIT_RATING, 25.0);
        survHunter.put(StatType.HIT_RATING, 23.0);
        survHunter.put(StatType.MANA_PER_5_SECONDS, 1.0);
        survHunter.put(StatType.STAMINA, 0.4);
        survHunter.put(StatType.ARMOR, 0.01);
        survHunter.put(StatType.WEAPON_DPS, 22.0);
        survHunter.put(StatType.WEAPON_SPEED, 10.0);
        registry.put(ClassSpec.HUNTER_SURVIVAL, new StatWeights(survHunter));

        // ==========================================
        // PRIEST
        // ==========================================
        Map<StatType, Double> discPriest = new EnumMap<>(StatType.class);
        discPriest.put(StatType.HEALING_POWER, 1.0);
        discPriest.put(StatType.SPELL_POWER, 0.85);
        discPriest.put(StatType.INTELLECT, 1.0);
        discPriest.put(StatType.SPIRIT, 0.9);
        discPriest.put(StatType.MANA_PER_5_SECONDS, 4.0);
        discPriest.put(StatType.SPELL_CRIT_RATING, 6.0);
        discPriest.put(StatType.STAMINA, 0.3);
        discPriest.put(StatType.ARMOR, 0.001);
        registry.put(ClassSpec.PRIEST_DISCIPLINE, new StatWeights(discPriest));

        Map<StatType, Double> holyPriest = new EnumMap<>(StatType.class);
        holyPriest.put(StatType.HEALING_POWER, 1.0);
        holyPriest.put(StatType.SPELL_POWER, 0.85);
        holyPriest.put(StatType.INTELLECT, 1.0);
        holyPriest.put(StatType.SPIRIT, 0.8);
        holyPriest.put(StatType.MANA_PER_5_SECONDS, 4.5);
        holyPriest.put(StatType.SPELL_CRIT_RATING, 6.0);
        holyPriest.put(StatType.STAMINA, 0.2);
        holyPriest.put(StatType.ARMOR, 0.001);
        registry.put(ClassSpec.PRIEST_HOLY, new StatWeights(holyPriest));

        Map<StatType, Double> shadowPriest = new EnumMap<>(StatType.class);
        shadowPriest.put(StatType.SPELL_POWER, 1.0);
        shadowPriest.put(StatType.INTELLECT, 1.0);
        shadowPriest.put(StatType.SPELL_HIT_RATING, 15.0);
        shadowPriest.put(StatType.SPELL_CRIT_RATING, 8.0);
        shadowPriest.put(StatType.MANA_PER_5_SECONDS, 2.0);
        shadowPriest.put(StatType.SPIRIT, 0.4);
        shadowPriest.put(StatType.STAMINA, 0.3);
        shadowPriest.put(StatType.ARMOR, 0.001);
        registry.put(ClassSpec.PRIEST_SHADOW, new StatWeights(shadowPriest));

        // ==========================================
        // SHAMAN
        // ==========================================
        Map<StatType, Double> eleShaman = new EnumMap<>(StatType.class);
        eleShaman.put(StatType.SPELL_POWER, 1.0);
        eleShaman.put(StatType.INTELLECT, 1.0);
        eleShaman.put(StatType.SPELL_CRIT_RATING, 9.0);
        eleShaman.put(StatType.SPELL_HIT_RATING, 13.0);
        eleShaman.put(StatType.MANA_PER_5_SECONDS, 3.0);
        eleShaman.put(StatType.SPIRIT, 0.1);
        eleShaman.put(StatType.ARMOR, 0.001);
        registry.put(ClassSpec.SHAMAN_ELEMENTAL, new StatWeights(eleShaman));

        Map<StatType, Double> enhShaman = new EnumMap<>(StatType.class);
        enhShaman.put(StatType.STRENGTH, 2.0);
        enhShaman.put(StatType.ATTACK_POWER, 1.0);
        enhShaman.put(StatType.AGILITY, 1.0);
        enhShaman.put(StatType.ARMOR, 0.01);
        enhShaman.put(StatType.CRIT_RATING, 22.0);
        enhShaman.put(StatType.HIT_RATING, 20.0);
        enhShaman.put(StatType.INTELLECT, 0.4);
        enhShaman.put(StatType.MANA_PER_5_SECONDS, 2.0);
        enhShaman.put(StatType.WEAPON_DPS, 12.5);
        enhShaman.put(StatType.WEAPON_SPEED, 32.0); // Hard requirement on massive slow two-handers for Windfury burst
        registry.put(ClassSpec.SHAMAN_ENHANCEMENT, new StatWeights(enhShaman));

        // Resto Shaman
        Map<StatType, Double> restoShaman = new EnumMap<>(StatType.class);
        restoShaman.put(StatType.HEALING_POWER, 1.0);
        restoShaman.put(StatType.SPELL_POWER, 0.85);
        restoShaman.put(StatType.INTELLECT, 1.0);
        restoShaman.put(StatType.MANA_PER_5_SECONDS, 4.5);
        restoShaman.put(StatType.SPIRIT, 0.1);
        restoShaman.put(StatType.SPELL_CRIT_RATING, 6.0);
        restoShaman.put(StatType.STAMINA, 0.3);
        restoShaman.put(StatType.ARMOR, 0.001);
        registry.put(ClassSpec.SHAMAN_RESTORATION, new StatWeights(restoShaman));

        // ==========================================
        // MAGE
        // ==========================================
        Map<StatType, Double> arcaneMage = new EnumMap<>(StatType.class);
        arcaneMage.put(StatType.SPELL_POWER, 1.0);
        arcaneMage.put(StatType.INTELLECT, 1.2);
        arcaneMage.put(StatType.SPELL_CRIT_RATING, 10.0);
        arcaneMage.put(StatType.SPELL_HIT_RATING, 14.0);
        arcaneMage.put(StatType.MANA_PER_5_SECONDS, 2.0);
        arcaneMage.put(StatType.SPIRIT, 0.3);
        arcaneMage.put(StatType.ARMOR, 0.001);
        registry.put(ClassSpec.MAGE_ARCANE, new StatWeights(arcaneMage));

        Map<StatType, Double> fireMage = new EnumMap<>(StatType.class);
        arcaneMage.put(StatType.SPELL_POWER, 1.0);
        fireMage.put(StatType.INTELLECT, 1.0);
        fireMage.put(StatType.SPELL_CRIT_RATING, 12.0);
        fireMage.put(StatType.SPELL_HIT_RATING, 15.0);
        fireMage.put(StatType.MANA_PER_5_SECONDS, 2.0);
        fireMage.put(StatType.SPIRIT, 0.2);
        fireMage.put(StatType.ARMOR, 0.001);
        registry.put(ClassSpec.MAGE_FIRE, new StatWeights(fireMage));

        Map<StatType, Double> frostMage = new EnumMap<>(StatType.class);
        arcaneMage.put(StatType.SPELL_POWER, 1.0);
        frostMage.put(StatType.INTELLECT, 1.0);
        frostMage.put(StatType.SPELL_CRIT_RATING, 11.0);
        frostMage.put(StatType.SPELL_HIT_RATING, 14.0);
        frostMage.put(StatType.MANA_PER_5_SECONDS, 2.0);
        frostMage.put(StatType.SPIRIT, 0.2);
        frostMage.put(StatType.ARMOR, 0.001);
        registry.put(ClassSpec.MAGE_FROST, new StatWeights(frostMage));

        // ==========================================
        // WARLOCK
        // ==========================================
        Map<StatType, Double> affLock = new EnumMap<>(StatType.class);
        affLock.put(StatType.SPELL_POWER, 1.0);
        affLock.put(StatType.INTELLECT, 1.0);
        affLock.put(StatType.SPELL_HIT_RATING, 14.0);
        affLock.put(StatType.SPELL_CRIT_RATING, 8.0);
        affLock.put(StatType.STAMINA, 0.5);
        affLock.put(StatType.ARMOR, 0.001);
        registry.put(ClassSpec.WARLOCK_AFFLICTION, new StatWeights(affLock));

        Map<StatType, Double> demoLock = new EnumMap<>(StatType.class);
        affLock.put(StatType.SPELL_POWER, 1.0);
        demoLock.put(StatType.INTELLECT, 1.0);
        demoLock.put(StatType.STAMINA, 0.8);
        demoLock.put(StatType.ARMOR, 0.001);
        demoLock.put(StatType.SPELL_CRIT_RATING, 8.0);
        demoLock.put(StatType.SPELL_HIT_RATING, 12.0);
        registry.put(ClassSpec.WARLOCK_DEMONOLOGY, new StatWeights(demoLock));

        Map<StatType, Double> destLock = new EnumMap<>(StatType.class);
        affLock.put(StatType.SPELL_POWER, 1.0);
        destLock.put(StatType.INTELLECT, 1.0);
        destLock.put(StatType.SPELL_CRIT_RATING, 11.0);
        destLock.put(StatType.SPELL_HIT_RATING, 15.0);
        destLock.put(StatType.STAMINA, 0.4);
        destLock.put(StatType.ARMOR, 0.001);
        registry.put(ClassSpec.WARLOCK_DESTRUCTION, new StatWeights(destLock));

        // ==========================================
        // DRUID
        // ==========================================
        Map<StatType, Double> balDruid = new EnumMap<>(StatType.class);
        balDruid.put(StatType.SPELL_POWER, 1.0);
        balDruid.put(StatType.INTELLECT, 1.0);
        balDruid.put(StatType.SPELL_CRIT_RATING, 10.0);
        balDruid.put(StatType.SPELL_HIT_RATING, 14.0);
        balDruid.put(StatType.MANA_PER_5_SECONDS, 3.0);
        balDruid.put(StatType.SPIRIT, 0.5);
        balDruid.put(StatType.ARMOR, 0.001);
        registry.put(ClassSpec.DRUID_BALANCE, new StatWeights(balDruid));

        Map<StatType, Double> feralDruid = new EnumMap<>(StatType.class);
        feralDruid.put(StatType.AGILITY, 1.4);
        feralDruid.put(StatType.STRENGTH, 1.2);
        feralDruid.put(StatType.ATTACK_POWER, 0.6);
        feralDruid.put(StatType.CRIT_RATING, 23.0);
        feralDruid.put(StatType.HIT_RATING, 20.0);
        feralDruid.put(StatType.STAMINA, 0.8);
        feralDruid.put(StatType.DEFENSE_RATING, 1.2);
        feralDruid.put(StatType.DODGE_RATING, 1.0);
        feralDruid.put(StatType.ARMOR, 0.12);
        // Feral Druids in Bear/Cat form completely override weapon dps/speed with standard form math.
        // Therefore, physical weapon properties remain completely unweighted for them in Classic.
        registry.put(ClassSpec.DRUID_FERAL, new StatWeights(feralDruid));

        Map<StatType, Double> restoDruid = new EnumMap<>(StatType.class);
        restoDruid.put(StatType.HEALING_POWER, 1.0);
        restoDruid.put(StatType.SPELL_POWER, 0.85);
        restoDruid.put(StatType.INTELLECT, 1.0);
        restoDruid.put(StatType.SPIRIT, 0.9);
        restoDruid.put(StatType.MANA_PER_5_SECONDS, 3.0);
        restoDruid.put(StatType.SPELL_CRIT_RATING, 5.0);
        restoDruid.put(StatType.STAMINA, 0.3);
        restoDruid.put(StatType.ARMOR, 0.001);
        registry.put(ClassSpec.DRUID_RESTORATION, new StatWeights(restoDruid));
    }

}
