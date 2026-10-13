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
    private final HandSlotUpgradeOrchestrator handSlotOrchestrator;

    public UpgradeFinderService(SingleSlotUpgradeOrchestrator singleSlotOrchestrator, DualSlotUpgradeOrchestrator dualSlotOrchestrator,
                                HandSlotUpgradeOrchestrator handSlotOrchestrator) {
        this.singleSlotOrchestrator = singleSlotOrchestrator;
        this.dualSlotOrchestrator = dualSlotOrchestrator;
        this.handSlotOrchestrator = handSlotOrchestrator;
    }

    public void findUpgradesFor(CharacterInfo info) {
        ISOLATED_SLOTS.forEach((slot) -> singleSlotOrchestrator.findUpgrade(slot, info));
        PAIRED_SLOTS.forEach(pair -> dualSlotOrchestrator.findUpgrades(pair, info));
        handSlotOrchestrator.findUpgrades(new DualSlot(MAIN_HAND, OFF_HAND), info);
    }
}
