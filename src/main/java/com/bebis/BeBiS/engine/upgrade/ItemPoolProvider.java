package com.bebis.BeBiS.engine.upgrade;

import com.bebis.BeBiS.engine.upgrade.domain.ItemPool;
import com.bebis.BeBiS.equipment.domain.Equipment;
import com.bebis.BeBiS.item.ItemQueryService;
import com.bebis.BeBiS.item.domain.Item;
import com.bebis.BeBiS.profile.domain.WowCharacter;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Builds the candidate pool for one slot and one character: items of a type the character can use (from the item
 * table, see {@link ItemQueryService}) that the character can also obtain (from the items' sources, see
 * {@link Item#isObtainableBy}).
 */
@Service
class ItemPoolProvider {

    private final ClassItemTypeCapabilitiesResolver itemTypeResolver;
    private final SlotItemTypeMatcher slotMatcher;

    private final ItemQueryService itemQueryService;

    ItemPoolProvider(ClassItemTypeCapabilitiesResolver itemTypeResolver, SlotItemTypeMatcher slotMatcher, ItemQueryService itemQueryService) {
        this.itemTypeResolver = itemTypeResolver;
        this.slotMatcher = slotMatcher;
        this.itemQueryService = itemQueryService;
    }

    /**
     * @return items for the slot that a character of this query can equip and obtain, unsorted
     */
    public ItemPool getItemPool(Equipment.Slot slot, Query query) {
        ClassItemTypeCapabilitiesResolver.ClassItemTypeCapabilities eligibleItemTypes =
                itemTypeResolver.getCapabilitiesFor(query.wowClass(), query.level()); // what the class is able to equip for given level

        ItemQueryService.ItemSearchCriteria criteria = new ItemQueryService.ItemSearchCriteria(slotMatcher.matchSlotToItemTypes(slot),
                eligibleItemTypes.eligibleArmor(), eligibleItemTypes.eligibleWeapons(), query.wowClass(), query.level());

        List<Item> itemPool = itemQueryService.findAll(criteria).stream()
                .filter((it) -> isItemEligibleFor(it, query.race(), query.wowClass()))
                .toList();

        return new ItemPool(itemPool);
    }

    public record Query(int level, WowCharacter.Race race, WowCharacter.WowClass wowClass) {
    }

    private boolean isItemEligibleFor(Item item, WowCharacter.Race race, WowCharacter.WowClass wowClass) {
        return item.isObtainableBy(race) && item.isObtainableBy(wowClass) && item.isEquippableBy(wowClass);
    }
}
