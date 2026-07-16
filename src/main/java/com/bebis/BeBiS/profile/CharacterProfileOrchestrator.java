package com.bebis.BeBiS.profile;

import com.bebis.BeBiS.equipment.EquipmentService;
import com.bebis.BeBiS.equipment.domain.Equipment;
import com.bebis.BeBiS.profile.domain.CharacterInfo;
import com.bebis.BeBiS.profile.domain.WowCharacter;
import com.bebis.BeBiS.profile.domain.WowTalents;
import com.bebis.BeBiS.profile.event.CharacterSelectedEvent;
import com.bebis.BeBiS.profile.jpa.WowCharacterEntity;
import com.bebis.BeBiS.profile.jpa.WowCharacterRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;


@Component
public class CharacterProfileOrchestrator {

    private final ApplicationEventPublisher eventPublisher;

    private final EquipmentService equipmentService;
    private final SpecializationService specService;

    private final ProfileMapper profileMapper;
    private final WowCharacterRepository characterRepository;

    public CharacterProfileOrchestrator(
            ApplicationEventPublisher eventPublisher,
            WowCharacterRepository characterRepository,
            ProfileMapper profileMapper,
            EquipmentService equipmentService,
            SpecializationService specService) {
        this.eventPublisher = eventPublisher;
        this.characterRepository = characterRepository;
        this.profileMapper = profileMapper;
        this.equipmentService = equipmentService;
        this.specService = specService;
    }

    @Transactional
    public CharacterInfo getCharacterInfo(long characterId, String realmSlug, long blizzardAccountId) {
        WowCharacterEntity.CompositeKey characterPk = new WowCharacterEntity.CompositeKey(characterId, realmSlug, blizzardAccountId);
        WowCharacterEntity characterEntity = characterRepository.findById(characterPk).get();
        WowCharacter character = profileMapper.mapToDomain(characterEntity);
        Optional<WowTalents> talents = specService.getTalentsForCharacter(character.wowCharacterId().realmSlug(), character.name());
        Equipment equipment = equipmentService.getEquipmentForCharacter(characterEntity);
        CharacterInfo characterInfo = new CharacterInfo(character, equipment, talents);
        eventPublisher.publishEvent(new CharacterSelectedEvent(characterInfo, Instant.now()));
        return characterInfo;
    }

}
