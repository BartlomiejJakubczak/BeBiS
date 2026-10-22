package com.bebis.BeBiS.profile;

import com.bebis.BeBiS.equipment.EquipmentService;
import com.bebis.BeBiS.equipment.domain.Equipment;
import com.bebis.BeBiS.equipment.jpa.EquipmentEntity;
import com.bebis.BeBiS.profile.domain.CharacterInfo;
import com.bebis.BeBiS.profile.domain.WowTalents.Specialization;
import com.bebis.BeBiS.profile.domain.WowCharacter;
import com.bebis.BeBiS.profile.domain.WowCharacterBuilder;
import com.bebis.BeBiS.profile.domain.WowTalents;
import com.bebis.BeBiS.profile.domain.exception.CharacterNotFoundException;
import com.bebis.BeBiS.profile.event.CharacterSelectedEvent;
import com.bebis.BeBiS.profile.jpa.WowCharacterEntity;
import com.bebis.BeBiS.profile.jpa.WowCharacterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Only the orchestrator's collaborators are mocked; the data passing through it is real.
 */
@ExtendWith(MockitoExtension.class)
class CharacterProfileOrchestratorTest {

    private static final WowCharacter.Id THELAMAR_ID = new WowCharacter.Id(21L, "soulseeker", 37L);

    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private EquipmentService equipmentService;
    @Mock
    private SpecializationService specService;
    @Mock
    private ProfileService profileService;
    @Mock
    private ProfileMapper profileMapper;
    @Mock
    private WowCharacterRepository characterRepository;

    private CharacterProfileOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new CharacterProfileOrchestrator(eventPublisher, characterRepository, profileMapper,
                equipmentService, specService, profileService);
    }

    @Test
    void shouldSelectCharacterWithSyncedEquipmentAndTalents_AndAnnounceTheSelection() {
        // given
        WowCharacterEntity storedCharacter = storedCharacter(THELAMAR_ID, "Thelamar");
        WowCharacter character = WowCharacterBuilder.newWowCharacter().withId(THELAMAR_ID).withName("Thelamar").build();
        Equipment equipment = new Equipment();
        Optional<WowTalents> talents = Optional.of(new WowTalents(List.of(
                new Specialization("COMBAT", 31), new Specialization("ASSASSINATION", 20), new Specialization("SUBTLETY", 0))));

        when(characterRepository.findById(WowCharacterEntity.CompositeKey.from(THELAMAR_ID))).thenReturn(Optional.of(storedCharacter));
        when(profileMapper.mapToDomain(storedCharacter)).thenReturn(character);
        when(equipmentService.getEquipmentForCharacter(storedCharacter.getEquipment())).thenReturn(equipment);
        when(specService.getTalentsForCharacter("soulseeker", "Thelamar")).thenReturn(talents);

        // when
        CharacterInfo result = orchestrator.selectCharacter(THELAMAR_ID);

        // then
        assertThat(result).isEqualTo(new CharacterInfo(character, equipment, talents));

        InOrder syncBeforeRead = inOrder(equipmentService);
        syncBeforeRead.verify(equipmentService).syncEquipment("Thelamar", "soulseeker", storedCharacter.getEquipment());
        syncBeforeRead.verify(equipmentService).getEquipmentForCharacter(storedCharacter.getEquipment());

        ArgumentCaptor<CharacterSelectedEvent> event = ArgumentCaptor.forClass(CharacterSelectedEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().characterInfo()).isEqualTo(result);
    }

    @Test
    void shouldThrowAndSelectNothing_WhenCharacterIsNotStored() {
        // given
        WowCharacter.Id unknownId = new WowCharacter.Id(404L, "soulseeker", 37L);
        when(characterRepository.findById(WowCharacterEntity.CompositeKey.from(unknownId))).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> orchestrator.selectCharacter(unknownId))
                .isInstanceOf(CharacterNotFoundException.class);
        verifyNoInteractions(equipmentService, specService, eventPublisher);
    }

    private static WowCharacterEntity storedCharacter(WowCharacter.Id id, String name) {
        WowCharacterEntity entity = new WowCharacterEntity();
        entity.setPk(WowCharacterEntity.CompositeKey.from(id));
        entity.setName(name);
        EquipmentEntity equipment = new EquipmentEntity();
        equipment.setCharacter(entity);
        entity.setEquipment(equipment);
        return entity;
    }
}
