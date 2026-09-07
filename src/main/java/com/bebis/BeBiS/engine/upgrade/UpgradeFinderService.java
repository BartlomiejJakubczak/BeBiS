package com.bebis.BeBiS.engine.upgrade;

import com.bebis.BeBiS.engine.upgrade.domain.DualSlot;
import com.bebis.BeBiS.equipment.domain.Equipment;
import com.bebis.BeBiS.profile.domain.CharacterInfo;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.bebis.BeBiS.equipment.domain.Equipment.Slot.*;

@Service
public class UpgradeFinderService {

    private static final List<Equipment.Slot> ISOLATED_SLOTS = List.of(HEAD, NECK, SHOULDER, BACK, CHEST, WRIST, HANDS,
            WAIST, LEGS, FEET, RANGED);

    private static final List<DualSlot> PAIRED_SLOTS = List.of(
            new DualSlot(TRINKET_1, TRINKET_2),
            new DualSlot(FINGER_1, FINGER_2)
    );

    private final SingleSlotUpgradeOrchestrator singleSlotOrchestrator;
    private final DualSlotUpgradeOrchestrator dualSlotOrchestrator;

    public UpgradeFinderService(SingleSlotUpgradeOrchestrator singleSlotOrchestrator, DualSlotUpgradeOrchestrator dualSlotOrchestrator) {
        this.singleSlotOrchestrator = singleSlotOrchestrator;
        this.dualSlotOrchestrator = dualSlotOrchestrator;
    }

    public void findUpgradesFor(CharacterInfo info) {
        PAIRED_SLOTS.forEach(pair -> dualSlotOrchestrator.findUpgrades(pair, info));
        // TODO main / off hand finder left

        ISOLATED_SLOTS.forEach((slot) -> singleSlotOrchestrator.findUpgrade(slot, info));
    }
}
