package com.bebis.BeBiS.engine.upgrade;

import com.bebis.BeBiS.engine.upgrade.domain.ItemPool;
import com.bebis.BeBiS.equipment.domain.Equipment.Slot;
import com.bebis.BeBiS.equipment.domain.EquippedItem;
import com.bebis.BeBiS.profile.domain.CharacterInfo;
import com.bebis.BeBiS.profile.domain.ClassSpec;
import com.bebis.BeBiS.profile.domain.WowCharacter;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import static com.bebis.BeBiS.equipment.domain.Equipment.Slot.*;
import static com.bebis.BeBiS.profile.domain.ClassSpec.getClassSpec;

/**
 * Runs the upgrade search for every isolated slot: builds one item pool for all of them, restricts it
 * per slot, asks {@link IsolatedSlotUpgradeFinder} for each slot's result and publishes it.
 */
@Component
class IsolatedSlotUpgradeOrchestrator implements UpgradeOrchestrator {

    private static final Set<Slot> SLOTS = EnumSet.of(HEAD, NECK, SHOULDER, BACK, CHEST, WRIST, HANDS,
            WAIST, LEGS, FEET, RANGED);

    private final IsolatedSlotUpgradeFinder upgradeFinder;
    private final ItemPoolProvider itemPoolProvider;
    private final UpgradeEventPublisher publisher;

    IsolatedSlotUpgradeOrchestrator(IsolatedSlotUpgradeFinder upgradeFinder, ItemPoolProvider itemPoolProvider,
                                    UpgradeEventPublisher publisher) {
        this.upgradeFinder = upgradeFinder;
        this.itemPoolProvider = itemPoolProvider;
        this.publisher = publisher;
    }

    @Override
    @Async
    public void findUpgradesFor(CharacterInfo info) {
        WowCharacter character = info.wowCharacter();
        ClassSpec classSpec = getClassSpec(character.wowClass(), info.talents());

        ItemPool itemPool = itemPoolProvider.getItemPool(SLOTS, new ItemPoolProvider.Query(character.level(), character.race(), character.wowClass()));

        SLOTS.forEach(slot -> {
            SlotUpgradeQuery query = new SlotUpgradeQuery(slot, info.equipment().getItem(slot),
                    itemPoolProvider.restrictToSlots(Set.of(slot), itemPool));
            publisher.publish(slot, upgradeFinder.findUpgrade(query, classSpec, character.race()));
        });
    }

    record SlotUpgradeQuery(Slot slot, Optional<EquippedItem> currentItem, ItemPool itemPool) {
    }
}
