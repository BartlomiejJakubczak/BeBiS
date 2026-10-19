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
import com.bebis.BeBiS.profile.domain.WowCharacter.WowClass;
import com.bebis.BeBiS.tools.EnumTools;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.bebis.BeBiS.item.domain.Item.InventoryType;
import static com.bebis.BeBiS.item.domain.Item.Quality;
import static com.bebis.BeBiS.tools.MapperTools.validateRequired;

@Component
public final class ItemMapper {

    public Item mapToDomain(ItemEntity entity) {
        Item.ItemMetadata meta = new Item.ItemMetadata(
                new Item.ItemKey(entity.getPk().getBaseId(), entity.getPk().getSuffixId()),
                entity.getName(),
                entity.getInventoryType(),
                entity.getQuality(),
                Optional.ofNullable(entity.getItemLevel()).orElse(0),
                Optional.ofNullable(entity.getRequiredLevel()).orElse(0),
                Optional.ofNullable(entity.getUniqueEquipped()).orElse(false),
                Map.copyOf(entity.getStats()),
                List.copyOf(entity.getSpecialEffects()),
                Set.copyOf(entity.getPlayableClasses())
        );

        return switch (entity) {
            case WeaponEntity w -> new Weapon(
                    meta,
                    Optional.ofNullable(w.getSpeed()).orElse(0.0),
                    Optional.ofNullable(w.getMinDamage()).orElse(0),
                    Optional.ofNullable(w.getMaxDamage()).orElse(0),
                    Optional.ofNullable(w.getDps()).orElse(0.0),
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

        if (weaponData == null)
            throw new InvalidItemException("weapon data cannot be null for weapon upgrade analysis purposes");
        if (weaponData.damage() == null)
            throw new InvalidItemException("weapon damage cannot be null for weapon upgrade analysis purposes");
        if (weaponData.attackSpeed() == null)
            throw new InvalidItemException("weapon speed cannot be null for weapon upgrade analysis purposes");
        if (weaponData.dps() == null)
            throw new InvalidItemException("weapon dps cannot be null for weapon upgrade analysis purposes");

        return new ItemSyncData(
                createCommonData(baseDTO, equippedItemDTO),
                new ItemSyncData.WeaponSyncData(
                        mapWeaponType((int) baseDTO.subclass().id()),
                        normalizeSpeed(weaponData.attackSpeed().value()),
                        weaponData.damage().minValue(),
                        weaponData.damage().maxValue(),
                        weaponData.dps().value()
                ),
                null
        );
    }

    private Double normalizeSpeed(double speed) {
        // Blizzard API sometimes returns speed in ms (1900) instead of seconds (1.9)
        return speed > 100 ? speed / 1000.0 : speed;
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
                mapRequiredLevel(baseDTO),
                mapUniqueEquipped(baseDTO),
                mapStats(baseDTO, equippedItemDTO),
                mapSpecialEffects(baseDTO),
                mapPlayableClasses(baseDTO)
        );
    }

    private Integer mapItemLevel(EquipmentResponse.ItemDTO equippedItemDTO) {
        return (equippedItemDTO.itemLevel()) != null ? equippedItemDTO.itemLevel().value() : null;
    }

    private Optional<Integer> mapArmorValue(ItemResponse baseDTO, EquipmentResponse.ItemDTO equippedItemDTO) {
        if (equippedItemDTO.armor() != null) return Optional.of(equippedItemDTO.armor().value());
        return mapArmorValueFromBase(baseDTO);
    }

    private Optional<Integer> mapArmorValueFromBase(ItemResponse baseDTO) {
        return (baseDTO.preview() != null && baseDTO.preview().armor() != null) ? Optional.of(baseDTO.preview().armor().value()) : Optional.empty();
    }

    private Map<StatType, Integer> mapStats(ItemResponse baseDTO, EquipmentResponse.ItemDTO dto) {
        Map<StatType, Integer> stats = new HashMap<>();

        mapArmorValue(baseDTO, dto).ifPresent(value -> stats.put(StatType.ARMOR, value)); // has to go first for gray / white items coverage

        List<EquipmentResponse.ItemDTO.StatDTO> statsFromDTO = dto.stats() != null ? dto.stats() : List.of();

        statsFromDTO.forEach(statDTO ->
                EnumTools.fromString(StatType.class, statDTO.type().type()).ifPresent(statType -> stats.put(statType, statDTO.value())));

        return stats;
    }

    private List<String> mapSpecialEffects(ItemResponse dto) {
        if (dto.preview() == null || dto.preview().spells() == null) {
            return List.of();
        } else {
            return dto.preview().spells().stream().map(ItemResponse.PreviewItemDTO.SpellEffectDTO::description).toList();
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

    // Blizzard's codes follow the client's INVTYPE_* constants (with 2HWEAPON spelled TWOHWEAPON), not our enum names,
    // so every code is mapped explicitly: renaming an enum constant must never silently change what the API maps to.
    private InventoryType mapInventoryType(ItemResponse dto) throws InvalidItemException {
        if (dto.inventoryType() != null && dto.inventoryType().type() != null) {
            String raw = dto.inventoryType().type();
            return switch (raw.toUpperCase()) {
                case "HEAD" -> InventoryType.HEAD;
                case "NECK" -> InventoryType.NECK;
                case "SHOULDER" -> InventoryType.SHOULDER;
                case "BODY" -> InventoryType.BODY;
                case "CHEST", "ROBE" -> InventoryType.CHEST; // ROBE is a chest piece that also covers the legs visually
                case "WAIST" -> InventoryType.WAIST;
                case "LEGS" -> InventoryType.LEGS;
                case "FEET" -> InventoryType.FEET;
                case "WRIST" -> InventoryType.WRIST;
                case "HAND" -> InventoryType.HANDS;
                case "FINGER" -> InventoryType.FINGER;
                case "TRINKET" -> InventoryType.TRINKET;
                case "CLOAK", "BACK" -> InventoryType.CLOAK; // BACK is a defensive alias, the API code is CLOAK
                case "TABARD" -> InventoryType.TABARD;
                case "WEAPON" -> InventoryType.WEAPON;
                case "TWOHWEAPON" -> InventoryType.TWO_HAND;
                case "WEAPONMAINHAND" -> InventoryType.WEAPONMAINHAND;
                case "WEAPONOFFHAND" -> InventoryType.WEAPONOFFHAND;
                case "SHIELD" -> InventoryType.SHIELD;
                case "HOLDABLE" -> InventoryType.HOLDABLE;
                case "RANGED" -> InventoryType.RANGED;
                case "RANGEDRIGHT" -> InventoryType.RANGEDRIGHT;
                case "THROWN" -> InventoryType.THROWN;
                case "RELIC" -> InventoryType.RELIC;
                default ->
                        throw new InvalidItemException("Could not map inventory type, reason: unsupported type " + raw);
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

    private int mapRequiredLevel(ItemResponse baseDTO) {
        return Optional.ofNullable(baseDTO.requiredLevel()).orElse(0);
    }

    private EnumSet<WowClass> mapPlayableClasses(ItemResponse baseDTO) throws InvalidItemException {
        if (baseDTO.preview() == null || baseDTO.preview().requirements() == null
                || baseDTO.preview().requirements().playableClasses() == null
                || baseDTO.preview().requirements().playableClasses().links() == null) {
            return EnumSet.noneOf(WowClass.class);
        }

        return baseDTO.preview().requirements().playableClasses().links().stream()
                .map(ItemResponse.RequirementsDTO.PlayableClassesDTO.PlayableClassDTO::id)
                .map((id) -> {
                    Optional<WowClass> wowClass = WowClass.fromId(id);
                    return wowClass.orElseThrow(() -> new InvalidItemException("Invalid playable class restriction, reason: " + id));
                })
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(WowClass.class)));
    }

}
