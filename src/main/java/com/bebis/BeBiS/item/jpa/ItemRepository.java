package com.bebis.BeBiS.item.jpa;

import com.bebis.BeBiS.item.domain.Armor;
import com.bebis.BeBiS.item.domain.Item;
import com.bebis.BeBiS.item.domain.Weapon;
import com.bebis.BeBiS.profile.domain.WowCharacter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface ItemRepository extends JpaRepository<ItemEntity, ItemEntity.CompositeKey> {

    @Query("""
            SELECT i FROM ItemEntity i
            WHERE i.inventoryType IN (:inventoryTypes)
              AND (i.armorType IS NULL OR i.armorType IN (:armorTypes))
              AND (i.weaponType IS NULL OR i.weaponType IN (:weaponTypes))
              AND (i.playableClasses IS EMPTY OR :playableClass MEMBER OF i.playableClasses)
              AND i.requiredLevel <= :level
            """)
    List<ItemEntity> findAllByEligibleItemTypesLevelAndClass(
            Set<Item.InventoryType> inventoryTypes,
            Set<Armor.ArmorType> armorTypes,
            Set<Weapon.WeaponType> weaponTypes,
            WowCharacter.WowClass playableClass,
            int level
    );
}
