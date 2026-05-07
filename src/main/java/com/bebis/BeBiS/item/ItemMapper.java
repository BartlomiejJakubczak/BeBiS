package com.bebis.BeBiS.item;

import com.bebis.BeBiS.integration.blizzard.dto.EquipmentResponse;
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
import java.util.Optional;

import static com.bebis.BeBiS.item.Item.InventoryType;
import static com.bebis.BeBiS.item.Item.Quality;

@Component
public class ItemMapper {

    public Item mapToDomain(ItemEntity entity) {
        Item.ItemMetadata meta = new Item.ItemMetadata(
                entity.getPk().getBaseId(),
                entity.getName(),
                entity.getInventoryType(),
                entity.getQuality(),
                Optional.ofNullable(entity.getItemLevel()).orElse(0),
                Optional.ofNullable(entity.getRequiredLevel()).orElse(0),
                Optional.ofNullable(entity.getUniqueEquipped()).orElse(false),
                entity.getStats() != null ? new HashMap<>(entity.getStats()) : new HashMap<>(),
                entity.getSpecialEffects() != null ? new ArrayList<>(entity.getSpecialEffects()) : new ArrayList<>()
        );

        return switch (entity) {
            case WeaponEntity w -> new Weapon(
                    meta,
                    Optional.ofNullable(w.getSpeed()).orElse(0.0),
                    Optional.ofNullable(w.getMinDamage()).orElse(0),
                    Optional.ofNullable(w.getMaxDamage()).orElse(0),
                    w.getWeaponType()
            );
            case ArmorEntity a -> new Armor(
                    meta,
                    Optional.ofNullable(a.getArmorValue()).orElse(0),
                    a.getArmorType()
            );
            default -> new EquippableItem(meta);
        };
    }

    public ItemSyncData mapToSyncData(ItemResponse baseDTO, EquipmentResponse.ItemDTO equippedItemDTO) {
        int classId = baseDTO.itemClass().id();
        int subclassId = (int) baseDTO.subclass().id();

        return switch (classId) {
            case 2 -> createWeaponSyncData(baseDTO, equippedItemDTO);
            case 4 ->
                    (subclassId == 0) ? createEquippableItemSyncData(baseDTO, equippedItemDTO) : createArmorSyncData(baseDTO, equippedItemDTO);
            default -> throw new IllegalArgumentException("Unsupported item class: " + classId);
        };
    }

    private ItemSyncData createWeaponSyncData(ItemResponse baseDTO, EquipmentResponse.ItemDTO equippedItemDTO) {
        var weaponData = equippedItemDTO.weapon();
        double speed = 0.0;
        int minDamage = 0;
        int maxDamage = 0;
        double dps = 0.0;
        if (weaponData != null) {
            speed = weaponData.attackSpeed().value();
            minDamage = weaponData.damage().minValue();
            maxDamage = weaponData.damage().maxValue();
            dps = weaponData.dps().value();
        }
        // Blizzard API sometimes returns speed in ms (1900) instead of seconds (1.9)
        return new ItemSyncData(
                baseDTO.id(),
                mapSuffixId(equippedItemDTO),
                validateRequired(equippedItemDTO.name(), "name"),
                mapQuality(equippedItemDTO.quality().type().toUpperCase()),
                mapInventoryType(baseDTO.inventoryType().type()),
                validateRequired(equippedItemDTO.itemLevel().value(), "item_level"),
                baseDTO.requiredLevel(),
                mapBoolean(baseDTO.preview().uniqueEquipped() != null),
                mapStats(equippedItemDTO),
                mapSpecialEffects(baseDTO), // those won't change even if "suffixed"
                mapArmorValue(baseDTO, equippedItemDTO), // some weapons might have armor
                null,
                speed > 100 ? speed / 1000.0 : speed,
                minDamage,
                maxDamage,
                dps,
                mapWeaponType((int) baseDTO.subclass().id())
        );
    }

    private ItemSyncData createArmorSyncData(ItemResponse baseDTO, EquipmentResponse.ItemDTO equippedItemDTO) {
        return new ItemSyncData(
                baseDTO.id(),
                mapSuffixId(equippedItemDTO),
                validateRequired(equippedItemDTO.name(), "name"),
                mapQuality(equippedItemDTO.quality().type().toUpperCase()),
                mapInventoryType(baseDTO.inventoryType().type()),
                validateRequired(equippedItemDTO.itemLevel().value(), "item_level"),
                baseDTO.requiredLevel(),
                mapBoolean(baseDTO.preview().uniqueEquipped() != null),
                mapStats(equippedItemDTO),
                mapSpecialEffects(baseDTO),
                mapArmorValue(baseDTO, equippedItemDTO),
                mapArmorType((int) baseDTO.subclass().id()),
                null,
                null,
                null,
                null,
                null
        );
    }

    private ItemSyncData createEquippableItemSyncData(ItemResponse baseDTO, EquipmentResponse.ItemDTO equippedItemDTO) {
        return new ItemSyncData(
                baseDTO.id(),
                mapSuffixId(equippedItemDTO),
                validateRequired(equippedItemDTO.name(), "name"),
                mapQuality(equippedItemDTO.quality().type().toUpperCase()),
                mapInventoryType(baseDTO.inventoryType().type()),
                validateRequired(equippedItemDTO.itemLevel().value(), "item_level"),
                baseDTO.requiredLevel(),
                mapBoolean(baseDTO.preview().uniqueEquipped() != null),
                mapStats(equippedItemDTO),
                mapSpecialEffects(baseDTO),
                mapArmorValue(baseDTO, equippedItemDTO), // some rings or trinkets might have armor
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    public long mapSuffixId(EquipmentResponse.ItemDTO equippedItemDTO) {
        if (equippedItemDTO.enchantments() == null || equippedItemDTO.name() == null) {
            return 0L;
        }
        return equippedItemDTO.enchantments().stream()
                .filter((ench) -> equippedItemDTO.name().endsWith(ench.displayString()) && ench.displayString().startsWith("of "))
                .map(EquipmentResponse.ItemDTO.EnchantmentDTO::enchantmentId)
                .findFirst()
                .orElse(0L);
    }

    private Integer mapArmorValue(ItemResponse baseDTO, EquipmentResponse.ItemDTO equippedItemDTO) {
        if (equippedItemDTO.armor() != null) return equippedItemDTO.armor().value();
        return mapArmorValueFromBase(baseDTO);
    }

    private Integer mapArmorValueFromBase(ItemResponse baseDTO) {
        return (baseDTO.preview().armor() != null) ? baseDTO.preview().armor().value() : null;
    }

    private Map<StatType, Integer> mapStats(EquipmentResponse.ItemDTO dto) {
        List<EquipmentResponse.ItemDTO.StatDTO> statsFromDTO = dto.stats();
        if (statsFromDTO == null || statsFromDTO.isEmpty()) {
            return new HashMap<>();
        } else {
            Map<StatType, Integer> stats = new HashMap<>();
            statsFromDTO.forEach(s -> {
                try {
                    stats.put(StatType.valueOf(s.type().type().toUpperCase()), s.value());
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

    private boolean mapBoolean(Boolean value) {
        return value != null && value;
    }

    private <T> T validateRequired(T value, String fieldName) {
        if (value == null) {
            throw new IllegalStateException("Critical data missing: " + fieldName);
        }
        return value;
    }
}