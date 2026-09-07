package com.bebis.BeBiS.engine.upgrade;

import com.bebis.BeBiS.engine.upgrade.domain.SlotPair;
import com.bebis.BeBiS.equipment.domain.Equipment;
import com.bebis.BeBiS.profile.domain.CharacterInfo;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.bebis.BeBiS.equipment.domain.Equipment.Slot.*;

@Service
public class UpgradeFinderService {

    private static final List<Equipment.Slot> ISOLATED_SLOTS = List.of(HEAD, NECK, SHOULDER, BACK, CHEST, WRIST, HANDS,
            WAIST, LEGS, FEET, RANGED);

    private static final List<SlotPair> PAIRED_SLOTS = List.of(
            new SlotPair(TRINKET_1, TRINKET_2),
            new SlotPair(FINGER_1, FINGER_2)
    );

    private final SingleSlotUpgradeOrchestrator singleSlotUpgradeOrchestrator;
    private final PairedUpgradeOrchestrator pairedOrchestrator;

    public UpgradeFinderService(SingleSlotUpgradeOrchestrator singleSlotUpgradeOrchestrator, PairedUpgradeOrchestrator pairedOrchestrator) {
        this.singleSlotUpgradeOrchestrator = singleSlotUpgradeOrchestrator;
        this.pairedOrchestrator = pairedOrchestrator;
    }

    public void findUpgradesFor(CharacterInfo info) {
        PAIRED_SLOTS.forEach(pair -> pairedOrchestrator.findUpgrades(pair, info));
        // TODO main / off hand finder left

        ISOLATED_SLOTS.forEach((slot) -> singleSlotUpgradeOrchestrator.findUpgrade(slot, info));
    }
}
