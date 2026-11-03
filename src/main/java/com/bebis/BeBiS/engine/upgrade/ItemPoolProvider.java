package com.bebis.BeBiS.engine.upgrade;

import com.bebis.BeBiS.engine.upgrade.domain.ItemPool;
import com.bebis.BeBiS.equipment.domain.Equipment.Slot;
import com.bebis.BeBiS.item.ItemQueryService;
import com.bebis.BeBiS.item.domain.Item;
import com.bebis.BeBiS.profile.domain.WowCharacter;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * Builds the candidate pool for one or more slots and one character: items of a type the character can use (from the
 * item table, see {@link ItemQueryService}) that the character can also obtain (from the items' sources, see
 * {@link Item#isObtainableBy}). A pool built for several slots can be narrowed down to each slot afterwards.
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
     * @return items for any of the slots that a character of this query can equip and obtain, unsorted
     */
    public ItemPool getItemPool(Set<Slot> slots, Query query) {
        ClassItemTypeCapabilitiesResolver.ClassItemTypeCapabilities eligibleItemTypes =
                itemTypeResolver.getCapabilitiesFor(query.wowClass(), query.level()); // what the class is able to equip for given level

        ItemQueryService.ItemSearchCriteria criteria = new ItemQueryService.ItemSearchCriteria(slotMatcher.matchSlotsToItemTypes(slots),
                eligibleItemTypes.eligibleArmor(), eligibleItemTypes.eligibleWeapons(), query.wowClass(), query.level());

        List<Item> itemPool = itemQueryService.findAll(criteria).stream()
                .filter((it) -> isItemEligibleFor(it, query))
                .toList();

        return new ItemPool(itemPool);
    }

    public ItemPool restrictToSlots(Set<Slot> slots, ItemPool pool) {
        return new ItemPool(restrictTo(slotMatcher.matchSlotsToItemTypes(slots), pool.items()));
    }

    private List<Item> restrictTo(Set<Item.InventoryType> targetTypes, List<Item> itemsFromPool) {
        return itemsFromPool.stream().filter(it -> targetTypes.contains(it.getMetadata().inventoryType())).toList();
    }

    public record Query(int level, WowCharacter.Race race, WowCharacter.WowClass wowClass) {
    }

    private boolean isItemEligibleFor(Item item, Query query) {
        return item.isObtainableBy(query.race()) && item.isObtainableBy(query.wowClass()) && item.isObtainableBy(query.level());
    }
}
