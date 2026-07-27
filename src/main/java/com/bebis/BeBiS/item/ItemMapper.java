package com.bebis.BeBiS.item;

import com.bebis.BeBiS.integration.blizzard.dto.EquipmentResponse;
import com.bebis.BeBiS.integration.blizzard.dto.ItemResponse;
import com.bebis.BeBiS.item.domain.Armor;
import com.bebis.BeBiS.item.domain.EquippableItem;
import com.bebis.BeBiS.item.domain.Item;
import com.bebis.BeBiS.item.domain.StatType;
import com.bebis.BeBiS.item.domain.Weapon;
import com.bebis.BeBiS.item.domain.exception.InvalidItemException;
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

import static com.bebis.BeBiS.item.domain.Item.InventoryType;
import static com.bebis.BeBiS.item.domain.Item.Quality;
import static com.bebis.BeBiS.tools.MapperTools.validateRequired;

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
                    a.getArmorType()
            );
            default -> new EquippableItem(meta);
        };
    }

    public List<Item> mapToDomain(List<ItemEntity> entities) {
        return entities.stream().map(this::mapToDomain).toList();
    }

    public ItemSyncData mapToSyncData(ItemResponse baseDTO, EquipmentResponse.ItemDTO equippedItemDTO) throws InvalidItemException {
        if (baseDTO != null && equippedItemDTO != null) {
            if (baseDTO.itemClass() == null || baseDTO.subclass() == null) {
                throw new InvalidItemException("classId and subclassId cannot be null for upgrade analysis purposes");
            }

            int classId = baseDTO.itemClass().id();
            int subclassId = (int) baseDTO.subclass().id();

            return switch (classId) {
                case 2 -> createWeaponSyncData(baseDTO, equippedItemDTO);
                case 4 ->
                        (subclassId == 0) ? createEquippableItemSyncData(baseDTO, equippedItemDTO) : createArmorSyncData(baseDTO, equippedItemDTO);
                default -> throw new InvalidItemException("Invalid classId: " + classId);
            };
        } else {
            throw new InvalidItemException("item and equippedItem responses cannot be null for upgrade analysis purposes");
        }
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
                createCommonData(baseDTO, equippedItemDTO),
                new ItemSyncData.WeaponSyncData(
                        mapWeaponType((int) baseDTO.subclass().id()),
                        speed > 100 ? speed / 1000.0 : speed,
                        minDamage, maxDamage, dps
                ),
                null
        );
    }

    private ItemSyncData createArmorSyncData(ItemResponse baseDTO, EquipmentResponse.ItemDTO equippedItemDTO) {
        return new ItemSyncData(
                createCommonData(baseDTO, equippedItemDTO),
                null,
                new ItemSyncData.ArmorSyncData(mapArmorType((int) baseDTO.subclass().id()))
        );
    }

    private ItemSyncData createEquippableItemSyncData(ItemResponse baseDTO, EquipmentResponse.ItemDTO equippedItemDTO) {
        return new ItemSyncData(createCommonData(baseDTO, equippedItemDTO), null, null);
    }

    private ItemSyncData.ItemSyncCommonData createCommonData(ItemResponse baseDTO, EquipmentResponse.ItemDTO equippedItemDTO) {
        return new ItemSyncData.ItemSyncCommonData(
                validateRequired(baseDTO.id(), "id", InvalidItemException::new),
                mapSuffixId(equippedItemDTO),
                validateRequired(equippedItemDTO.name(), "name", InvalidItemException::new),
                mapQuality(equippedItemDTO),
                mapInventoryType(baseDTO),
                mapItemLevel(equippedItemDTO),
                validateRequired(baseDTO.requiredLevel(), "required_level", InvalidItemException::new),
                mapUniqueEquipped(baseDTO),
                mapStats(baseDTO, equippedItemDTO),
                mapSpecialEffects(baseDTO)
        );
    }

    private Integer mapItemLevel(EquipmentResponse.ItemDTO equippedItemDTO) {
        return (equippedItemDTO.itemLevel()) != null ? equippedItemDTO.itemLevel().value() : null;
    }

    private Integer mapArmorValue(ItemResponse baseDTO, EquipmentResponse.ItemDTO equippedItemDTO) {
        if (equippedItemDTO.armor() != null) return equippedItemDTO.armor().value();
        return mapArmorValueFromBase(baseDTO);
    }

    private Integer mapArmorValueFromBase(ItemResponse baseDTO) {
        return (baseDTO.preview().armor() != null) ? baseDTO.preview().armor().value() : null;
    }

    private Map<StatType, Integer> mapStats(ItemResponse baseDTO, EquipmentResponse.ItemDTO dto) {
        Map<StatType, Integer> stats = new HashMap<>();
        stats.put(StatType.ARMOR, mapArmorValue(baseDTO, dto));

        List<EquipmentResponse.ItemDTO.StatDTO> statsFromDTO = dto.stats();
        if (statsFromDTO == null || statsFromDTO.isEmpty()) {
            return stats;
        }

        statsFromDTO.forEach(s -> {
            try {
                stats.put(StatType.valueOf(s.type().type().toUpperCase()), s.value());
            } catch (IllegalArgumentException ignored) {
            }
        });
        return stats;
    }

    private List<String> mapSpecialEffects(ItemResponse dto) {
        if (dto.preview() == null || dto.preview().spells() == null) {
            return new ArrayList<>();
        } else {
            List<ItemResponse.PreviewItemDTO.SpellEffectDTO> spellsFromDTO = dto.preview().spells();
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

    private InventoryType mapInventoryType(ItemResponse dto) throws InvalidItemException {
        if (dto.inventoryType() != null && dto.inventoryType().type() != null) {
            String raw = dto.inventoryType().type();
            return switch (raw.toUpperCase()) {
                case "WEAPON" -> InventoryType.WEAPON;
                case "WEAPONMAINHAND" -> InventoryType.WEAPONMAINHAND;
                case "ROBE" -> InventoryType.CHEST;
                case "BACK" -> InventoryType.CLOAK;
                default -> {
                    try {
                        yield InventoryType.valueOf(raw.toUpperCase());
                    } catch (Exception e) {
                        throw new InvalidItemException("Could not map inventory type, reason: " + e.getMessage());
                    }
                }
            };
        }
        throw new InvalidItemException("inventoryType cannot be null for upgrade analysis purposes");
    }

    private Quality mapQuality(EquipmentResponse.ItemDTO dto) {
        if (dto.quality() != null && dto.quality().type() != null) {
            String raw = dto.quality().type().toUpperCase();
            try {
                return Quality.valueOf(raw.toUpperCase());
            } catch (Exception e) {
                return Quality.UNKNOWN;
            }
        }
        return Quality.UNKNOWN; // quality is not that important, no need to throw exception
    }

    private boolean mapUniqueEquipped(ItemResponse baseDTO) {
        return baseDTO.preview() != null && baseDTO.preview().uniqueEquipped() != null; // no value means not unique
    }

}