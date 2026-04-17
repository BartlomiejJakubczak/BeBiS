package com.bebis.BeBiS.equipment;

import com.bebis.BeBiS.integration.blizzard.dto.ItemResponse;

import java.util.ArrayList;
import java.util.List;

import static com.bebis.BeBiS.integration.blizzard.dto.EquipmentResponse.ItemDTO;

public class EquipmentTestData {

    public static ItemDTO fromItemResponseSuffixed(
            ItemResponse itemResponse,
            String slot,
            String qualityType,
            String suffixName,  // "of the Tiger" (can be null)
            long suffixId,
            int level,
            List<ItemDTO.StatDTO> stats,
            List<ItemDTO.EnchantmentDTO> enchs // Usually just player enchants here
    ) {

        String baseName = itemResponse.name();
        String fullName = baseName;
        List<ItemDTO.EnchantmentDTO> finalEnchs = new ArrayList<>(enchs);

        if (suffixName != null && !suffixName.isBlank() && suffixId != 0L) {
            fullName = baseName + " " + suffixName;
            finalEnchs.add(new ItemDTO.EnchantmentDTO(suffixId, suffixName));
        }

        return new ItemDTO(
                new ItemDTO.ItemDTOReference(itemResponse.id()),
                new ItemDTO.SlotDTO(slot),
                fullName,
                new ItemDTO.QualityDTO(qualityType),
                new ItemDTO.LevelDTO(level),
                stats,
                mapArmorDTO(itemResponse.preview().armor()),
                mapWeaponDTO(itemResponse.preview().weapon()),
                finalEnchs
        );
    }

    public static ItemDTO fromItemResponseNoSuffix(
            ItemResponse itemResponse,
            String slot,
            List<ItemDTO.EnchantmentDTO> enchs
    ) {
        return new ItemDTO(
                new ItemDTO.ItemDTOReference(itemResponse.id()),
                new ItemDTO.SlotDTO(slot),
                itemResponse.name(),
                new ItemDTO.QualityDTO(itemResponse.quality().type()),
                new ItemDTO.LevelDTO(itemResponse.itemLevel()),
                mapBaseStats(itemResponse.preview().stats()),
                mapArmorDTO(itemResponse.preview().armor()),
                mapWeaponDTO(itemResponse.preview().weapon()),
                enchs
        );
    }

    public static ItemDTO.StatDTO stat(String type, int value) {
        return new ItemDTO.StatDTO(new ItemDTO.StatDTO.StatTypeWrapper(type), value);
    }

    public static ItemDTO.EnchantmentDTO enchant(long id, String display) {
        return new ItemDTO.EnchantmentDTO(id, display);
    }

    private static List<ItemDTO.StatDTO> mapBaseStats(List<ItemResponse.StatDTO> baseStats) {
        if (baseStats == null) {
            return null;
        }
        return baseStats.stream().map(s -> (stat(s.type().type(), s.value()))).toList();
    }

    private static ItemDTO.WeaponDTO mapWeaponDTO(ItemResponse.WeaponDTO weaponDTO) {
        if (weaponDTO == null) {
            return null;
        }
        return new ItemDTO.WeaponDTO(
                new ItemDTO.WeaponDTO.DamageDTO(
                        weaponDTO.damage().minValue(),
                        weaponDTO.damage().maxValue()
                ),
                new ItemDTO.WeaponDTO.AttackSpeedDTO(weaponDTO.attackSpeed().value()),
                new ItemDTO.WeaponDTO.DpsDTO(weaponDTO.dps().value())
        );
    }

    private static ItemDTO.ArmorDTO mapArmorDTO(ItemResponse.ArmorDTO armorDTO) {
        if (armorDTO == null) {
            return null;
        }
        return new ItemDTO.ArmorDTO(armorDTO.value());
    }
}
