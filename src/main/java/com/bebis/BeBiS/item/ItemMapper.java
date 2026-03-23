package com.bebis.BeBiS.item;

import com.bebis.BeBiS.integration.blizzard.dto.ItemResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.bebis.BeBiS.item.Item.*;

@Component
public class ItemMapper {

    public Item map(ItemResponse dto) {
        int classId = dto.itemClass().id();
        int subclassId = (int) dto.subclass().id();

        if (classId == 2) {
            return mapToWeapon(dto);
        } else if (classId == 4) {
            // VITAL: Rings, Necks, Trinkets, and caster Off-hands are Class 4 (Armor), Subclass 0 (Misc).
            // They DO NOT have armor blocks in the Blizzard JSON. Calling dto.preview().armor() on them will NPE.
            if (subclassId == 0) {
                return mapToEquippable(dto);
            }
            return mapToArmor(dto);
        } else {
            // Fast-fail for non-equippables (Bags, Potions, Quest Items)
            throw new IllegalArgumentException("Item is not equippable: " + dto.name());
        }
    }

    private Weapon mapToWeapon(ItemResponse dto) {
        ItemMetadata meta = mapMetadata(dto);

        var weaponData = dto.preview().weapon();
        double rawSpeed = weaponData.attackSpeed().value();
        double speed = (rawSpeed > 100) ? rawSpeed / 1000.0 : rawSpeed;
        int min = weaponData.damage().minValue();
        int max = weaponData.damage().maxValue();

        Weapon.WeaponType type = switch ((int) dto.subclass().id()) {
            case 0, 1 -> Weapon.WeaponType.AXE;
            case 2 -> Weapon.WeaponType.BOW;
            case 3 -> Weapon.WeaponType.GUN;
            case 4, 5 -> Weapon.WeaponType.MACE;
            case 6 -> Weapon.WeaponType.POLEARM;
            case 7, 8 -> Weapon.WeaponType.SWORD;
            case 10 -> Weapon.WeaponType.STAFF;
            case 15 -> Weapon.WeaponType.DAGGER;
            case 19 -> Weapon.WeaponType.WAND;
            default -> Weapon.WeaponType.UNARMED;
        };

        return new Weapon(meta, speed, min, max, type);
    }

    private Armor mapToArmor(ItemResponse dto) {
        ItemMetadata meta = mapMetadata(dto);

        // VITAL: Armor data lives inside the preview block
        int value = dto.preview().armor().value();

        Armor.ArmorType type = switch ((int) dto.subclass().id()) {
            case 1 -> Armor.ArmorType.CLOTH;
            case 2 -> Armor.ArmorType.LEATHER;
            case 3 -> Armor.ArmorType.MAIL;
            case 4 -> Armor.ArmorType.PLATE;
            case 6 -> Armor.ArmorType.SHIELD;
            default -> Armor.ArmorType.MISC;
        };

        return new Armor(meta, value, type);
    }

    private EquippableItem mapToEquippable(ItemResponse dto) {
        return new EquippableItem(mapMetadata(dto));
    }

    private ItemMetadata mapMetadata(ItemResponse dto) {
        Map<StatType, Integer> statsMap = new HashMap<>();
        List<String> effects = new ArrayList<>();
        boolean isUnique = false;

        // VITAL: All rich tooltip data (stats, spells, uniqueness) is nested in preview_item.
        // We must null-check the preview block just in case a grey item has no tooltip data.
        if (dto.preview() != null) {
            if (dto.preview().stats() != null) {
                for (var s : dto.preview().stats()) {
                    try {
                        statsMap.put(StatType.valueOf(s.type().type()), s.value());
                    } catch (IllegalArgumentException ignored) {
                        // Ignores unknown stats
                    }
                }
            }

            if (dto.preview().spells() != null) {
                dto.preview().spells().forEach(s -> effects.add(s.description()));
            }

            // In Classic Era, if this string exists at all, the item is restricted.
            isUnique = dto.preview().uniqueEquipped() != null;
        }

        String rawType = dto.inventoryType().type().toUpperCase();
        InventoryType invType;
        try {
            invType = switch (rawType) {
                case "WEAPON" -> InventoryType.WEAPON;
                case "WEAPONMAINHAND" -> InventoryType.WEAPONMAINHAND;
                case "ROBE" -> InventoryType.CHEST;
                default -> InventoryType.valueOf(rawType);
            };
        } catch (IllegalArgumentException e) {
            invType = InventoryType.UNKNOWN;
        }

        return new Item.ItemMetadata(
                dto.id(),
                dto.name(),
                invType,
                Quality.valueOf(dto.quality().type().toUpperCase()),
                dto.itemLevel(),
                dto.requiredLevel(),
                isUnique,
                statsMap,
                effects
        );
    }
}