package com.bebis.BeBiS.equipment;

import com.bebis.BeBiS.equipment.jpa.EquipmentEntity;
import com.bebis.BeBiS.integration.blizzard.BlizzardUserClient;
import com.bebis.BeBiS.integration.blizzard.dto.EquipmentResponse;
import com.bebis.BeBiS.profile.jpa.WowCharacterEntity;
import com.bebis.BeBiS.profile.jpa.WowCharacterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EquipmentService {

    private final BlizzardUserClient blizzardUserClient;
    private final EquipmentMapper equipmentMapper;
    private final EquipmentSynchronizer equipmentSynchronizer;

    private final WowCharacterRepository wowCharacterRepository;

    public EquipmentService(
            BlizzardUserClient blizzardUserClient,
            EquipmentMapper equipmentMapper,
            EquipmentSynchronizer equipmentSynchronizer,
            WowCharacterRepository wowCharacterRepository) {
        this.blizzardUserClient = blizzardUserClient;
        this.equipmentMapper = equipmentMapper;
        this.equipmentSynchronizer = equipmentSynchronizer;
        this.wowCharacterRepository = wowCharacterRepository;
    }

    @Transactional // defines a unit of work for transactions
    public Equipment getEquipmentForCharacter(long characterId, String realmSlug, long blizzardAccountId) {
        WowCharacterEntity.CompositeKey characterPk = new WowCharacterEntity.CompositeKey(characterId, realmSlug, blizzardAccountId);
        WowCharacterEntity character = wowCharacterRepository.findById(characterPk).get(); // I'm getting the eq here, both managed
        EquipmentEntity equipment = character.getEquipment();
        // freshEquipment is the source of truth for current char's gear
        EquipmentResponse freshEquipment = blizzardUserClient.getCharacterEquipment(character.getPk().getRealmSlug(), character.getName());
        equipmentSynchronizer.synchronize(freshEquipment, equipment); // no need to call eqRepo.save, because equipment is in managed state
        return equipmentMapper.mapToDomain(equipment);
    }

}
