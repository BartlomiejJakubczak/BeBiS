package com.bebis.BeBiS.profile.jpa;

import com.bebis.BeBiS.equipment.jpa.EquipmentEntity;
import com.bebis.BeBiS.profile.dto.CharacterSyncData;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WowCharacterEntityFactory {

    public WowCharacterEntity createNewCharacter(CharacterSyncData fresh) {
        WowCharacterEntity entity = new WowCharacterEntity();
        entity.setPk(new WowCharacterEntity.CompositeKey(
                fresh.characterId(),
                fresh.realmSlug(),
                fresh.blizzardAccountId()
        ));

        EquipmentEntity equipment = new EquipmentEntity();
        equipment.setCharacter(entity);
        entity.setEquipment(equipment);

        updateEntityFromSyncData(entity, fresh);
        return entity;
    }

    public List<WowCharacterEntity> createNewCharacters(List<CharacterSyncData> fresh) {
        return fresh.stream().map(this::createNewCharacter).toList();
    }

    private void updateEntityFromSyncData(WowCharacterEntity entity, CharacterSyncData fresh) {
        entity.updateFrom(fresh);
    }
}
