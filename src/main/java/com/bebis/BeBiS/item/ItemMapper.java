package com.bebis.BeBiS.item;

import com.bebis.BeBiS.integration.blizzard.dto.ItemResponse;
import com.bebis.BeBiS.item.dto.ItemSyncData;
import com.bebis.BeBiS.item.jpa.ArmorEntity;
import com.bebis.BeBiS.item.jpa.ItemEntity;
import com.bebis.BeBiS.item.jpa.WeaponEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.bebis.BeBiS.item.Item.InventoryType;
import static com.bebis.BeBiS.item.Item.Quality;

@Component
public class ItemMapper {

    public Item mapToDomain(ItemEntity entity) {
        Item.ItemMetadata meta = new Item.ItemMetadata(
                entity.getId().getItemId(),
                entity.getName(),
                entity.getInventoryType(),
                entity.getQuality(),
                entity.getItemLevel(),
                entity.getRequiredLevel(),
                entity.isUniqueEquipped(),
                entity.getStats() != null ? new HashMap<>(entity.getStats()) : new HashMap<>(),
                entity.getSpecialEffects() != null ? new ArrayList<>(entity.getSpecialEffects()) : new ArrayList<>()
        );

        return switch (entity) {
            case WeaponEntity w ->
                    new Weapon(meta, w.getSpeed(), w.getMinDamage(), w.getMaxDamage(), w.getWeaponType());
            case ArmorEntity a -> new Armor(meta, a.getArmorValue(), a.getArmorType());
            default -> new EquippableItem(meta);
        };
    }

    public ItemSyncData mapToSyncData(ItemResponse dto, long enchId) {
        int classId = dto.itemClass().id();
        int subclassId = (int) dto.subclass().id();

        return switch (classId) {
            case 2 -> createWeaponSyncData(dto, enchId);
            case 4 -> (subclassId == 0) ? createEquippableItemSyncData(dto, enchId) : createArmorSyncData(dto, enchId);
            default -> throw new IllegalArgumentException("Unsupported item class: " + classId);
        };
    }

    private ItemSyncData createWeaponSyncData(ItemResponse dto, long enchId) {
        var weaponData = dto.preview().weapon();
        double speed = weaponData.attackSpeed().value();
        // Blizzard API sometimes returns speed in ms (1900) instead of seconds (1.9)
        return new ItemSyncData(
                dto.id(),
                enchId,
                dto.name(),
                mapQuality(dto.quality().type().toUpperCase()),
                mapInventoryType(dto.inventoryType().type()),
                dto.itemLevel(),
                dto.requiredLevel(),
                dto.preview().uniqueEquipped() != null,
                mapStats(dto),
                mapSpecialEffects(dto),
                extractArmorValue(dto), // some weapons might have armor
                null,
                speed > 100 ? speed / 1000.0 : speed,
                weaponData.damage().minValue(),
                weaponData.damage().maxValue(),
                weaponData.dps().value(),
                mapWeaponType((int) dto.subclass().id())
        );
    }

    private ItemSyncData createArmorSyncData(ItemResponse dto, long enchId) {
        return new ItemSyncData(
                dto.id(),
                enchId,
                dto.name(),
                mapQuality(dto.quality().type().toUpperCase()),
                mapInventoryType(dto.inventoryType().type()),
                dto.itemLevel(),
                dto.requiredLevel(),
                dto.preview().uniqueEquipped() != null,
                mapStats(dto),
                mapSpecialEffects(dto),
                extractArmorValue(dto),
                mapArmorType((int) dto.subclass().id()),
                null,
                null,
                null,
                null,
                null
        );
    }

    private ItemSyncData createEquippableItemSyncData(ItemResponse dto, long enchId) {
        return new ItemSyncData(
                dto.id(),
                enchId,
                dto.name(),
                mapQuality(dto.quality().type().toUpperCase()),
                mapInventoryType(dto.inventoryType().type()),
                dto.itemLevel(),
                dto.requiredLevel(),
                dto.preview().uniqueEquipped() != null,
                mapStats(dto),
                mapSpecialEffects(dto),
                extractArmorValue(dto), // some rings or trinkets might have armor
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private Integer extractArmorValue(ItemResponse dto) {
        return (dto.preview().armor() != null) ? dto.preview().armor().value() : null;
    }

    private Map<StatType, Integer> mapStats(ItemResponse dto) {
        List<ItemResponse.StatDTO> statsFromDTO = dto.preview().stats();
        if (statsFromDTO.isEmpty()) {
            return new HashMap<>();
        } else {
            Map<StatType, Integer> stats = new HashMap<>();
            statsFromDTO.forEach(s -> {
                try {
                    stats.put(StatType.valueOf(s.type().type()), s.value());
                } catch (IllegalArgumentException ignored) {
                }
            });
            return stats;
        }
    }

    private List<String> mapSpecialEffects(ItemResponse dto) {
        List<ItemResponse.PreviewItemDTO.SpellEffectDTO> spellsFromDTO = dto.preview().spells();
        if (spellsFromDTO.isEmpty()) {
            return new ArrayList<>();
        } else {
            List<String> specialEffects = new ArrayList<>();
            spellsFromDTO.forEach(
                    s -> specialEffects.add(s.description()));
            return specialEffects;
        }
    }

    private Weapon.WeaponType mapWeaponType(int subclassId) {
        return switch (subclassId) {
            case 0, 1 -> Weapon.WeaponType.AXE;       // 0: 1H, 1: 2H
            case 2 -> Weapon.WeaponType.BOW;
            case 3 -> Weapon.WeaponType.GUN;
            case 4, 5 -> Weapon.WeaponType.MACE;      // 4: 1H, 5: 2H
            case 6 -> Weapon.WeaponType.POLEARM;
            case 7, 8 -> Weapon.WeaponType.SWORD;     // 7: 1H, 8: 2H
            case 10 -> Weapon.WeaponType.STAFF;
            case 15 -> Weapon.WeaponType.DAGGER;
            case 18 -> Weapon.WeaponType.CROSSBOW;
            case 19 -> Weapon.WeaponType.WAND;
            default -> Weapon.WeaponType.UNARMED;
        };
    }

    private Armor.ArmorType mapArmorType(int subclassId) {
        return switch (subclassId) {
            case 1 -> Armor.ArmorType.CLOTH;
            case 2 -> Armor.ArmorType.LEATHER;
            case 3 -> Armor.ArmorType.MAIL;
            case 4 -> Armor.ArmorType.PLATE;
            case 6 -> Armor.ArmorType.SHIELD;
            default -> Armor.ArmorType.MISC;          // Covers Class 4 Subclass 0 (Rings/Necks)
        };
    }

    private InventoryType mapInventoryType(String raw) {
        return switch (raw.toUpperCase()) {
            case "WEAPON" -> InventoryType.WEAPON;
            case "WEAPONMAINHAND" -> InventoryType.WEAPONMAINHAND;
            case "ROBE" -> InventoryType.CHEST;
            case "BACK" -> InventoryType.CLOAK;
            default -> {
                try {
                    yield InventoryType.valueOf(raw.toUpperCase());
                } catch (Exception e) {
                    yield InventoryType.UNKNOWN;
                }
            }
        };
    }

    private Quality mapQuality(String raw) {
        try {
            return Quality.valueOf(raw.toUpperCase());
        } catch (Exception e) {
            return Quality.UNKNOWN;
        }
    }
}