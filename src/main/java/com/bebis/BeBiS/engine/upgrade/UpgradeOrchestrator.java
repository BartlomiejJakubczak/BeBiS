package com.bebis.BeBiS.engine.upgrade;

import com.bebis.BeBiS.profile.domain.CharacterInfo;

/**
 * Finds and publishes upgrades for the group of slots it owns. {@link UpgradeFinderService} runs every orchestrator,
 * so together they must cover each {@link com.bebis.BeBiS.equipment.domain.Equipment.Slot} exactly once.
 */
interface UpgradeOrchestrator {

    void findUpgradesFor(CharacterInfo info);
}
