package com.bebis.BeBiS.equipment;

import com.bebis.BeBiS.equipment.domain.Equipment;
import com.bebis.BeBiS.equipment.jpa.EquipmentEntity;
import com.bebis.BeBiS.integration.blizzard.BlizzardUserClient;
import com.bebis.BeBiS.integration.blizzard.dto.EquipmentResponse;
import com.bebis.BeBiS.profile.jpa.WowCharacterEntity;
import org.springframework.stereotype.Service;

@Service
public class EquipmentService {

    private final BlizzardUserClient blizzardUserClient;
    private final EquipmentMapper equipmentMapper;
    private final EquipmentSynchronizer equipmentSynchronizer;

    public EquipmentService(
            BlizzardUserClient blizzardUserClient,
            EquipmentMapper equipmentMapper,
            EquipmentSynchronizer equipmentSynchronizer) {
        this.blizzardUserClient = blizzardUserClient;
        this.equipmentMapper = equipmentMapper;
        this.equipmentSynchronizer = equipmentSynchronizer;
    }

    public Equipment getEquipmentForCharacter(WowCharacterEntity characterEntity) {
        EquipmentEntity equipment = characterEntity.getEquipment();
        // freshEquipment is the source of truth for current char's gear
        EquipmentResponse freshEquipment = blizzardUserClient.getCharacterEquipment(characterEntity.getPk().getRealmSlug(), characterEntity.getName());
        equipmentSynchronizer.synchronize(freshEquipment, equipment); // no need to call eqRepo.save, because equipment is in managed state
        return equipmentMapper.mapToDomain(equipment);
    }

}
