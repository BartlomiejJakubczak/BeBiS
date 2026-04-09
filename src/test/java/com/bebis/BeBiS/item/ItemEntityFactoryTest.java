package com.bebis.BeBiS.item;

import com.bebis.BeBiS.item.dto.ItemSyncData;
import com.bebis.BeBiS.item.jpa.ArmorEntity;
import com.bebis.BeBiS.item.jpa.EquippableItemEntity;
import com.bebis.BeBiS.item.jpa.ItemEntity;
import com.bebis.BeBiS.item.jpa.ItemEntityFactory;
import com.bebis.BeBiS.item.jpa.WeaponEntity;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Collections;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class ItemEntityFactoryTest {

    private final ItemEntityFactory factory = new ItemEntityFactory();

    @ParameterizedTest(name = "Should create {1} when input has weaponType={2} and armorType={3}")
    @MethodSource("itemBranchingProvider")
    void shouldCreateCorrectSubtype(
            ItemSyncData input,
            Class<? extends ItemEntity> expectedClass,
            Weapon.WeaponType wType,
            Armor.ArmorType aType) {

        // when
        ItemEntity result = factory.createItemEntity(input);

        // then
        assertThat(result).isExactlyInstanceOf(expectedClass);
        if (result instanceof WeaponEntity weapon) {
            assertThat(weapon.getWeaponType()).isEqualTo(wType);
        } else if (result instanceof ArmorEntity armor) {
            assertThat(armor.getArmorType()).isEqualTo(aType);
        }
    }

    private static Stream<Arguments> itemBranchingProvider() {
        return Stream.of(
                // Case 1: Weapon (WeaponType present)
                Arguments.of(
                        createSyncData(Weapon.WeaponType.SWORD, null),
                        WeaponEntity.class, Weapon.WeaponType.SWORD, null
                ),
                // Case 2: Armor (ArmorType present, WeaponType null)
                Arguments.of(
                        createSyncData(null, Armor.ArmorType.PLATE),
                        ArmorEntity.class, null, Armor.ArmorType.PLATE
                ),
                // Case 3: Equippable Fallback (Both null - e.g. a Ring)
                Arguments.of(
                        createSyncData(null, null),
                        EquippableItemEntity.class, null, null
                ),
                // Case 4: Priority Check (Both present - should be Weapon)
                Arguments.of(
                        createSyncData(Weapon.WeaponType.AXE, Armor.ArmorType.LEATHER),
                        WeaponEntity.class, Weapon.WeaponType.AXE, Armor.ArmorType.LEATHER
                )
        );
    }

    // Helper to keep the MethodSource readable
    private static ItemSyncData createSyncData(Weapon.WeaponType wType, Armor.ArmorType aType) {
        return new ItemSyncData(
                123L, 0L, "Test Item", Item.Quality.EPIC, Item.InventoryType.HEAD,
                80, 60, true, Collections.emptyMap(), Collections.emptyList(),
                100, aType, 2.6, 100, 200, 50.0, wType
        );
    }
}
