package com.bebis.BeBiS.item;

import com.bebis.BeBiS.integration.blizzard.dto.ItemResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ItemMapperTest {
    private final ItemMapper itemMapper = new ItemMapper();

    @Test
    void shouldMapWeaponCorrectly() {
        // given
        ItemResponse response = ItemTestData.thunderfuryResponse();
        // when
        Item result = itemMapper.map(response);
        // then
        assertInstanceOf(Weapon.class, result, "Should be an instance of Weapon");
        Weapon weapon = (Weapon) result;
        assertEquals(1.9, weapon.getSpeed());
        assertEquals(82, weapon.getMinDamage());
        assertEquals(Weapon.WeaponType.SWORD, weapon.getWeaponType());
    }

    @Test
    void shouldMapArmorCorrectly() {
        // given - Subclass 4 is Plate
        ItemResponse response = ItemTestData.armorResponse(12345, "Breastplate", 500, 4);
        // when
        Item result = itemMapper.map(response);
        // then
        assertInstanceOf(Armor.class, result, "Should be an instance of Armor");
        Armor armor = (Armor) result;
        assertEquals(500, armor.getArmorValue());
        assertEquals(Armor.ArmorType.PLATE, armor.getArmorType());
    }

    @Test
    void shouldMapGenericEquippableItem() {
        // given - A Ring (class id 0 or anything not 2/4)
        ItemResponse response = ItemTestData.genericResponse(123, "Seal of Wrynn", "FINGER");
        // when
        Item result = itemMapper.map(response);
        // then
        assertInstanceOf(EquippableItem.class, result, "Should fall back to EquippableItem");
        assertEquals(Item.InventoryType.FINGER, result.getMetadata().inventoryType());
    }

    @Test
    void shouldHandleUnknownStatTypesGracefully() {
        // given
        ItemResponse response = ItemTestData.thunderfuryResponse();
        // Add a fake stat that doesn't exist in our Enum
        response.preview().stats().add(new ItemResponse.StatDTO(
                new ItemResponse.StatDTO.StatTypeWrapper("GHOST_STAT"), 99));

        // when & then
        assertDoesNotThrow(() -> itemMapper.map(response),
                "Mapper should ignore stats it doesn't recognize");
    }
}