package com.bebis.BeBiS.item;

import com.bebis.BeBiS.item.domain.Armor;
import com.bebis.BeBiS.item.domain.Item;
import com.bebis.BeBiS.item.domain.Weapon;
import com.bebis.BeBiS.item.jpa.ItemEntity;
import com.bebis.BeBiS.item.jpa.ItemRepository;
import com.bebis.BeBiS.item.jpa.QuestRewardEntity;
import com.bebis.BeBiS.item.jpa.QuestRewardRepository;
import com.bebis.BeBiS.profile.domain.WowCharacter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Read side of the item feature: the one place where other features get fully assembled domain {@link Item}s.
 * <p>
 * Facts that don't come from Blizzard, such as quest rewards, live in their own tables, which are joined in memory:
 * load the items, load the related rows for all of them with a single {@code IN} query, then group the rows by base id
 * and hand each item its rows. This keeps the number of queries constant, no matter how many items are loaded.
 * <p>
 * Not {@code final}: {@code @Transactional} needs Spring to subclass this bean into a proxy.
 */
@Service
public class ItemQueryService {

    private final ItemRepository itemRepository;
    private final QuestRewardRepository questRewardRepository;
    private final ItemMapper itemMapper;

    public ItemQueryService(ItemRepository itemRepository, QuestRewardRepository questRewardRepository, ItemMapper itemMapper) {
        this.itemRepository = itemRepository;
        this.questRewardRepository = questRewardRepository;
        this.itemMapper = itemMapper;
    }

    /**
     * Finds the items matching the criteria, together with their sources. Only filters on the items' own columns;
     * whether a character can obtain them is up to the caller (see {@link Item#isObtainableBy}).
     */
    @Transactional(readOnly = true)
    public List<Item> findAll(ItemSearchCriteria criteria) {
        List<ItemEntity> items = itemRepository.findAllByEligibleItemTypesLevelAndClass(criteria.inventoryTypes(),
                criteria.armorTypes(), criteria.weaponTypes(), criteria.playableClass(), criteria.level());
        if (items.isEmpty()) {
            return List.of();
        }

        return sourceWithQuestRewardsData(items);
    }

    /**
     * Assembles the items with the given keys, together with their sources, e.g. a character's equipped items. Keys
     * without a stored item are left out of the result.
     *
     * @return the items by key, so a caller can place the same item into several slots (two identical rings)
     */
    @Transactional(readOnly = true)
    public Map<Item.ItemKey, Item> findAllByKeys(Set<Item.ItemKey> keys) {
        List<ItemEntity> items = itemRepository.findAllById(convert(keys));
        if (items.isEmpty()) {
            return Map.of();
        }

        List<Item> sourcedItems = sourceWithQuestRewardsData(items);

        return sourcedItems.stream().collect(Collectors.toMap(
                (it) -> it.getMetadata().key(),
                (it) -> it
        ));
    }

    private List<Item> sourceWithQuestRewardsData(List<ItemEntity> items) {
        Map<Long, List<QuestRewardEntity>> questRewardsByBaseId = questRewardRepository.findAllByBaseIds(extractBaseIds(items))
                .stream()
                .collect(Collectors.groupingBy(reward -> reward.getPk().getBaseId()));

        return items.stream()
                .map(item -> itemMapper.mapToDomain(item, questRewardsByBaseId.getOrDefault(item.getPk().getBaseId(), List.of())))
                .toList();
    }

    /**
     * What to search the item table for.
     *
     * @param inventoryTypes item types that fit the slot
     * @param armorTypes     armor types the character can wear, items that aren't armor always pass
     * @param weaponTypes    weapon types the character can use, items that aren't weapons always pass
     * @param playableClass  the character's class, checked against the item's equip restriction
     * @param level          the character's level, checked against the item's required level
     */
    public record ItemSearchCriteria(
            Set<Item.InventoryType> inventoryTypes,
            Set<Armor.ArmorType> armorTypes,
            Set<Weapon.WeaponType> weaponTypes,
            WowCharacter.WowClass playableClass,
            int level
    ) {
    }

    private Set<Long> extractBaseIds(List<ItemEntity> items) {
        return items.stream().map(item -> item.getPk().getBaseId()).collect(Collectors.toSet());
    }

    private List<ItemEntity.CompositeKey> convert(Set<Item.ItemKey> keys) {
        return keys.stream().map(ItemEntity.CompositeKey::from).collect(Collectors.toList());
    }
}
