package com.bebis.BeBiS.engine.upgrade;

import com.bebis.BeBiS.engine.upgrade.event.UpgradeEvent;
import com.bebis.BeBiS.equipment.domain.Equipment;
import com.bebis.BeBiS.item.ItemMapper;
import com.bebis.BeBiS.item.domain.Item;
import com.bebis.BeBiS.item.jpa.ItemEntity;
import com.bebis.BeBiS.item.jpa.ItemRepository;
import com.bebis.BeBiS.profile.domain.CharacterInfo;
import com.bebis.BeBiS.profile.domain.WowCharacter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.bebis.BeBiS.engine.upgrade.ClassItemTypeCapabilitiesResolver.ClassItemTypeCapabilities;

@Component
public class IsolatedUpgradeOrchestrator {
    // TODO make it and others where applicable package-private during /integration test package removal.

    private final ClassItemTypeCapabilitiesResolver itemTypeResolver;
    private final SlotItemTypeMatcher slotMatcher;

    private final IsolatedUpgradeFinder isolatedUpgradeFinder;

    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;

    private final ApplicationEventPublisher publisher;

    IsolatedUpgradeOrchestrator(ClassItemTypeCapabilitiesResolver itemTypeResolver, SlotItemTypeMatcher slotMatcher,
                                IsolatedUpgradeFinder isolatedUpgradeFinder, ItemRepository itemRepository,
                                ItemMapper itemMapper, ApplicationEventPublisher publisher) {
        this.itemTypeResolver = itemTypeResolver;
        this.slotMatcher = slotMatcher;
        this.isolatedUpgradeFinder = isolatedUpgradeFinder;
        this.itemRepository = itemRepository;
        this.itemMapper = itemMapper;
        this.publisher = publisher;
    }

    @Async
    @Transactional(readOnly = true)
    public void findUpgrade(Equipment.Slot slot, CharacterInfo info) {
        List<Item> itemPool = getItemPool(slot, info.wowCharacter().level(), info.wowCharacter().wowClass());
        switch (isolatedUpgradeFinder.findUpgrade(info.equipment().getItem(slot), info.wowCharacter(), info.talents(), itemPool)) {
            case UpgradeResult.UpgradeFoundResult result ->
                    publisher.publishEvent(new UpgradeEvent.UpgradeFoundEvent(slot, result.item(), result.timestamp()));
            case UpgradeResult.AlreadyBiSResult result ->
                    publisher.publishEvent(new UpgradeEvent.BiSAlreadyEquippedEvent(slot, result.timestamp()));
            case UpgradeResult.NoItemsAvailableResult result ->
                    publisher.publishEvent(new UpgradeEvent.UpgradeNotAvailableEvent(slot, result.timestamp()));
        }
    }

    private List<Item> getItemPool(Equipment.Slot slot, int level, WowCharacter.WowClass wowClass) {
        ClassItemTypeCapabilities eligibleItemTypes = itemTypeResolver.getCapabilitiesFor(wowClass, level); // what the class is able to equip for given level
        List<ItemEntity> allEligibleItems = itemRepository.findAllByEligibleItemTypesAndLevel(slotMatcher.matchSlotToItemTypes(slot),
                eligibleItemTypes.eligibleArmor(), eligibleItemTypes.eligibleWeapons(), level);
        return itemMapper.mapToDomain(allEligibleItems);
    }

}
