package com.bebis.BeBiS.engine.upgrade;

import com.bebis.BeBiS.equipment.domain.Equipment;
import com.bebis.BeBiS.item.ItemMapper;
import com.bebis.BeBiS.item.domain.Item;
import com.bebis.BeBiS.item.jpa.ItemEntity;
import com.bebis.BeBiS.item.jpa.ItemRepository;
import com.bebis.BeBiS.profile.domain.WowCharacter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
class ItemPoolProvider {

    private final ClassItemTypeCapabilitiesResolver itemTypeResolver;
    private final SlotItemTypeMatcher slotMatcher;

    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;

    ItemPoolProvider(ClassItemTypeCapabilitiesResolver itemTypeResolver, SlotItemTypeMatcher slotMatcher,
                     ItemRepository itemRepository, ItemMapper itemMapper) {
        this.itemTypeResolver = itemTypeResolver;
        this.slotMatcher = slotMatcher;
        this.itemRepository = itemRepository;
        this.itemMapper = itemMapper;
    }

    @Transactional(readOnly = true)
    public List<Item> getItemPool(Equipment.Slot slot, int level, WowCharacter.WowClass wowClass) {
        ClassItemTypeCapabilitiesResolver.ClassItemTypeCapabilities eligibleItemTypes = itemTypeResolver.getCapabilitiesFor(wowClass, level); // what the class is able to equip for given level
        List<ItemEntity> allEligibleItems = itemRepository.findAllByEligibleItemTypesAndLevel(slotMatcher.matchSlotToItemTypes(slot),
                eligibleItemTypes.eligibleArmor(), eligibleItemTypes.eligibleWeapons(), level);
        return itemMapper.mapToDomain(allEligibleItems);
    }
}
