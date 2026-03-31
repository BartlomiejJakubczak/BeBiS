package com.bebis.BeBiS.item;

import com.bebis.BeBiS.integration.blizzard.dto.ItemResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.bebis.BeBiS.integration.blizzard.dto.ItemResponse.*;
import static com.bebis.BeBiS.item.Item.*;

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
                        new WeaponDTO.AttackSpeedDTO(1.9),
                        new WeaponDTO.DamageDTO(82, 153),
                        new WeaponDTO.DpsDTO(61.84)
                ),
                null, // No armor on weapons
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

    public static ItemResponse armorResponse(long id, String name, int armorValue, int subclassId) {
        var preview = new PreviewItemDTO(
                new ArrayList<>(), null, new ArmorDTO(armorValue), new ArrayList<>(), null
        );

        return new ItemResponse(
                id, name, new QualityDTO("RARE"), 40, 40,
                new ItemClassDTO(4, "Armor"),
                new SubclassDTO(subclassId, "Material"),
                new InventoryTypeDTO("CHEST"),
                preview
        );
    }

    // Generates a Ring/Trinket (Class 4, Subclass 0)
    public static ItemResponse genericResponse(long id, String name, String type) {
        var preview = new PreviewItemDTO(
                new ArrayList<>(), null, null, new ArrayList<>(), null
        );

        return new ItemResponse(
                id, name, new QualityDTO("COMMON"), 10, 1,
                new ItemClassDTO(4, "Armor"), // Rings are class 4
                new SubclassDTO(0, "Misc"),   // Subclass 0 prevents NPE
                new InventoryTypeDTO(type),
                preview
        );
    }

    public static Item thunderfury() {
        ItemMetadata metadata = new ItemMetadata(
                THUNDERFURY_ID,
                THUNDERFURY_NAME,
                InventoryType.WEAPON,
                Quality.LEGENDARY,
                80, 60, true,
                Map.of(
                        StatType.AGILITY, 5,
                        StatType.STAMINA, 8
                ),
                List.of(TF_EFFECT)
        );
        return new Weapon(metadata, 1.9, 82, 153, Weapon.WeaponType.SWORD);
    }
}