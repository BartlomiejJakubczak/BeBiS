package com.bebis.BeBiS.item;

import com.bebis.BeBiS.integration.blizzard.dto.ItemResponse;

import java.util.ArrayList;
import java.util.List;

import static com.bebis.BeBiS.integration.blizzard.dto.ItemResponse.*;

public class ItemTestData {
    public static final long THUNDERFURY_ID = 19019;
    public static final String THUNDERFURY_NAME = "Thunderfury, Blessed Blade of the Windseeker";
    public static final String TF_EFFECT = "Chance on hit: Blasts your enemy with lightning, dealing 300 Nature damage " +
            "and then jumping to additional nearby enemies.  Each jump reduces that victim's Nature resistance by 25. " +
            "Affects 5 targets. Your primary target is also consumed by a cyclone, slowing its attack speed by 20% for 12 sec.";

    public static ItemResponse thunderfuryResponse() {
        // Construct the nested preview block
        var preview = new PreviewItemDTO(
                new ArrayList<>(List.of(
                        new StatDTO(new StatDTO.StatTypeWrapper("AGILITY"), 5),
                        new StatDTO(new StatDTO.StatTypeWrapper("STAMINA"), 8)
                )),
                new WeaponDTO(
                        new WeaponDTO.AttackSpeedDTO(1900),
                        new WeaponDTO.DamageDTO(82, 153),
                        new WeaponDTO.DpsDTO(61.84)
                ),
                null,
                new ArrayList<>(List.of(new PreviewItemDTO.SpellEffectDTO(TF_EFFECT))),
                "Unique" // Marks it as true
        );

        return new ItemResponse(
                THUNDERFURY_ID, THUNDERFURY_NAME,
                new QualityDTO("LEGENDARY"), 80, 60,
                new ItemClassDTO(2, "Weapon"),
                new SubclassDTO(7, "Sword"),
                new InventoryTypeDTO("WEAPON"),
                preview
        );
    }

    public static ItemResponse createDtoWithNulls() {
        // A Preview block where everything optional is explicitly null
        var preview = new ItemResponse.PreviewItemDTO(
                new ArrayList<>(), // Empty stats list
                null,              // No weapon data
                null,              // No armor data
                new ArrayList<>(), // No spells
                null               // unique_equipped is NULL
        );

        return new ItemResponse(
                999L,
                "Simple String Ring",
                new ItemResponse.QualityDTO("COMMON"),
                1, 1,
                new ItemResponse.ItemClassDTO(4, "Armor"),
                new ItemResponse.SubclassDTO(0, "Misc"),
                new ItemResponse.InventoryTypeDTO("FINGER"),
                preview
        );
    }

    public static ItemResponse createDtoWithGarbageEnums(String quality, String inventoryType) {
        var preview = new ItemResponse.PreviewItemDTO(
                new ArrayList<>(), // Empty stats list
                null,              // No weapon data
                null,              // No armor data
                new ArrayList<>(), // No spells
                null               // unique_equipped is NULL
        );
        return new ItemResponse(
                999L,
                "Simple String Ring",
                new ItemResponse.QualityDTO(quality),
                1, 1,
                new ItemResponse.ItemClassDTO(4, "Armor"),
                new ItemResponse.SubclassDTO(0, "Misc"),
                new ItemResponse.InventoryTypeDTO(inventoryType),
                preview
        );
    }

    public static ItemResponse armorResponse(long id, String name, int armorValue) {
        var preview = new PreviewItemDTO(
                new ArrayList<>(), null, new ArmorDTO(armorValue), new ArrayList<>(), null
        );

        return new ItemResponse(
                id, name, new QualityDTO("RARE"), 40, 40,
                new ItemClassDTO(4, "Armor"),
                new SubclassDTO(4, "Plate"), // 0 means ring
                new InventoryTypeDTO("CHEST"),
                preview
        );
    }

    // Generates a Ring/Trinket (Class 4, Subclass 0)
    public static ItemResponse equippableItemResponse(long id, String name, String type, Integer armorValue) {
        ArmorDTO armorDTO = (armorValue != null) ? new ArmorDTO(armorValue) : null;
        var preview = new PreviewItemDTO(
                new ArrayList<>(), null, armorDTO, new ArrayList<>(), null
        );

        return new ItemResponse(
                id, name, new QualityDTO("COMMON"), 10, 1,
                new ItemClassDTO(4, "Armor"), // Rings are class 4
                new SubclassDTO(0, "Misc"),   // Subclass 0 prevents NPE
                new InventoryTypeDTO(type),
                preview
        );
    }
}