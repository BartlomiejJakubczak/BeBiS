package com.bebis.BeBiS.profile;

import com.bebis.BeBiS.equipment.EquipmentService;
import com.bebis.BeBiS.equipment.domain.Equipment;
import com.bebis.BeBiS.equipment.jpa.EquipmentEntity;
import com.bebis.BeBiS.profile.domain.CharacterInfo;
import com.bebis.BeBiS.profile.domain.WowCharacter;
import com.bebis.BeBiS.profile.domain.WowTalents;
import com.bebis.BeBiS.profile.domain.exception.CharacterNotFoundException;
import com.bebis.BeBiS.profile.event.CharacterSelectedEvent;
import com.bebis.BeBiS.profile.jpa.WowCharacterEntity;
import com.bebis.BeBiS.profile.jpa.WowCharacterRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;


@Component
public class CharacterProfileOrchestrator {

    private final ApplicationEventPublisher eventPublisher;

    private final EquipmentService equipmentService;
    private final SpecializationService specService;
    private final ProfileService profileService;

    private final ProfileMapper profileMapper;
    private final WowCharacterRepository characterRepository;

    public CharacterProfileOrchestrator(
            ApplicationEventPublisher eventPublisher,
            WowCharacterRepository characterRepository,
            ProfileMapper profileMapper,
            EquipmentService equipmentService,
            SpecializationService specService,
            ProfileService profileService) {
        this.eventPublisher = eventPublisher;
        this.characterRepository = characterRepository;
        this.profileMapper = profileMapper;
        this.equipmentService = equipmentService;
        this.specService = specService;
        this.profileService = profileService;
    }

    @Transactional
    public List<WowCharacter> getProfileSummary(long blizzardAccountId) {
        return profileService.getProfileSummary(blizzardAccountId);
    }

    /**
     * Selects a character for analysis: refreshes its equipment from Blizzard, loads its talents, publishes a
     * {@link CharacterSelectedEvent} (which starts the upgrade search after commit) and returns what was selected.
     *
     * @throws CharacterNotFoundException if no stored character has this id, e.g. a stale or tampered request
     */
    @Transactional
    public CharacterInfo selectCharacter(WowCharacter.Id characterId) {
        CharacterInfo characterInfo = characterRepository.findById(WowCharacterEntity.CompositeKey.from(characterId)).map((entity) -> {
            WowCharacter character = profileMapper.mapToDomain(entity);
            return new CharacterInfo(character, processEquipment(entity), processTalents(character));
        }).orElseThrow(() -> new CharacterNotFoundException("No character by given id: " + characterId + " was found."));

        eventPublisher.publishEvent(new CharacterSelectedEvent(characterInfo, Instant.now()));
        return characterInfo;
    }

    private Equipment processEquipment(WowCharacterEntity entity) {
        EquipmentEntity eq = entity.getEquipment();
        equipmentService.syncEquipment(entity.getName(), entity.getPk().getRealmSlug(), eq);
        return equipmentService.getEquipmentForCharacter(eq);
    }

    private WowTalents processTalents(WowCharacter character) {
        return specService.getTalentsForCharacter(character.id().realmSlug(), character.name());
    }

}
