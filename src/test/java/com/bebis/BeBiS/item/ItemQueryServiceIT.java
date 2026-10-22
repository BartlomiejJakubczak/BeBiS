package com.bebis.BeBiS.item;

import com.bebis.BeBiS.base.BaseFullStackTest;
import com.bebis.BeBiS.item.domain.Armor;
import com.bebis.BeBiS.item.domain.Item;
import com.bebis.BeBiS.item.domain.QuestSource;
import com.bebis.BeBiS.item.domain.Weapon;
import com.bebis.BeBiS.item.jpa.ArmorEntity;
import com.bebis.BeBiS.item.jpa.EquippableItemEntity;
import com.bebis.BeBiS.item.jpa.ItemEntity;
import com.bebis.BeBiS.item.jpa.WeaponEntity;
import com.bebis.BeBiS.profile.domain.WowCharacter;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs against the real quest reward seed (V4), so these ids are actual VMaNGOS quest rewards.
 */
class ItemQueryServiceIT extends BaseFullStackTest {

    private static final long SWORD_OF_OMEN = 6802L; // choice reward of "Into The Scarlet Monastery" (1048)
    private static final long MARBLED_BUCKLER = 6725L; // rewarded by both faction versions of a Razorfen Downs quest
    private static final long BAND_OF_ACCURIA = 17063L; // raid drop, not a quest reward

    @Autowired
    private ItemQueryService service;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldAssembleItemsWithAllTheirQuestSources_LoadingQuestRewardsForTheWholeBatch() {
        // given
        persist(sword(), shield(), ring());

        // when
        Map<Long, Item> result = service.findAll(criteriaMatchingEverything()).stream()
                .collect(Collectors.toMap(item -> item.getMetadata().key().baseId(), Function.identity()));

        // then
        assertThat(result).containsOnlyKeys(SWORD_OF_OMEN, MARBLED_BUCKLER, BAND_OF_ACCURIA);
        assertThat(result.get(SWORD_OF_OMEN).getMetadata().sources())
                .singleElement()
                .isInstanceOfSatisfying(QuestSource.class, quest -> {
                    assertThat(quest.questId()).isEqualTo(1048L);
                    assertThat(quest.minLevel()).isEqualTo(33);
                });
        assertThat(result.get(MARBLED_BUCKLER).getMetadata().sources())
                .extracting(source -> ((QuestSource) source).questId())
                .containsExactlyInAnyOrder(1101L, 1102L);
        assertThat(result.get(BAND_OF_ACCURIA).getMetadata().sources()).isEmpty();
    }

    @Test
    void shouldReturnNoItems_WhenNothingMatches() {
        // given / when
        List<Item> result = service.findAll(criteriaMatchingEverything());

        // then
        assertThat(result).isEmpty();
    }

    private ItemQueryService.ItemSearchCriteria criteriaMatchingEverything() {
        return new ItemQueryService.ItemSearchCriteria(EnumSet.allOf(Item.InventoryType.class),
                EnumSet.allOf(Armor.ArmorType.class), EnumSet.allOf(Weapon.WeaponType.class), WowCharacter.WowClass.WARRIOR, 60);
    }

    private void persist(ItemEntity... entities) {
        for (ItemEntity entity : entities) {
            entityManager.persist(entity);
        }
        entityManager.flush();
        entityManager.clear();
    }

    private static WeaponEntity sword() {
        WeaponEntity sword = new WeaponEntity();
        setMetadata(sword, SWORD_OF_OMEN, "Sword of Omen", Item.InventoryType.WEAPON, 0);
        sword.setWeaponType(Weapon.WeaponType.SWORD);
        return sword;
    }

    private static ArmorEntity shield() {
        ArmorEntity shield = new ArmorEntity();
        setMetadata(shield, MARBLED_BUCKLER, "Marbled Buckler", Item.InventoryType.SHIELD, 0);
        shield.setArmorType(Armor.ArmorType.SHIELD);
        return shield;
    }

    private static EquippableItemEntity ring() {
        EquippableItemEntity ring = new EquippableItemEntity();
        setMetadata(ring, BAND_OF_ACCURIA, "Band of Accuria", Item.InventoryType.FINGER, 60);
        return ring;
    }

    private static void setMetadata(ItemEntity entity, long baseId, String name, Item.InventoryType type, int requiredLevel) {
        entity.setPk(new ItemEntity.CompositeKey(baseId, 0L));
        entity.setName(name);
        entity.setQuality(Item.Quality.RARE);
        entity.setInventoryType(type);
        entity.setRequiredLevel(requiredLevel);
        entity.setStats(Map.of());
        entity.setSpecialEffects(List.of());
        entity.setPlayableClasses(Set.of());
    }
}
