package com.bebis.BeBiS.engine.upgrade;

import com.bebis.BeBiS.equipment.domain.Equipment;
import com.bebis.BeBiS.profile.domain.CharacterInfo;
import org.springframework.stereotype.Service;

import static com.bebis.BeBiS.equipment.domain.Equipment.Slot.*;

@Service
public class UpgradeFinderService {

    private final IsolatedUpgradeOrchestrator isolatedUpgradeOrchestrator;

    public UpgradeFinderService(IsolatedUpgradeOrchestrator isolatedUpgradeOrchestrator) {
        this.isolatedUpgradeOrchestrator = isolatedUpgradeOrchestrator;
    }

    public void findUpgradesFor(CharacterInfo info) {
        for (Equipment.Slot slot : values()) {
            if (slot.equals(MAIN_HAND) || slot.equals(OFF_HAND)) {
                // TODO if weapon: create a separate class that looks at main/off hand combinations for classSpec
                // weaponUpgradeFinder.findUpgrade().....
            } else {
                // armor, rings, trinkets and ranged
                isolatedUpgradeOrchestrator.findUpgrade(slot, info);
            }
        }
    }
}
