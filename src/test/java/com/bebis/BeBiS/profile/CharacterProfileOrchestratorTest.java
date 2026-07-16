package com.bebis.BeBiS.profile;

import com.bebis.BeBiS.equipment.EquipmentService;
import com.bebis.BeBiS.equipment.domain.Equipment;
import com.bebis.BeBiS.profile.domain.CharacterInfo;
import com.bebis.BeBiS.profile.domain.WowCharacter;
import com.bebis.BeBiS.profile.domain.WowTalents;
import com.bebis.BeBiS.profile.event.CharacterSelectedEvent;
import com.bebis.BeBiS.profile.jpa.WowCharacterEntity;
import com.bebis.BeBiS.profile.jpa.WowCharacterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CharacterProfileOrchestratorTest {

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
    void setup() {
        orchestrator = new CharacterProfileOrchestrator(eventPublisher, characterRepository, profileMapper, equipmentService, specService, profileService);
    }

    @Test
    void shouldGetCharacterInfo() {
        // given
        long charId = 1L;
        String realmSlug = "soulseeker";
        String characterName = "Thelamar";
        long blizzId = 1L;

        WowCharacterEntity.CompositeKey charPk = new WowCharacterEntity.CompositeKey(charId, realmSlug, blizzId);

        WowCharacterEntity mockEntity = mock(WowCharacterEntity.class);

        WowCharacter mockDomain = mock(WowCharacter.class);
        when(mockDomain.name()).thenReturn(characterName);
        when(mockDomain.wowCharacterId()).thenReturn(new WowCharacter.Id(charId, realmSlug));

        Equipment mockEquipment = mock(Equipment.class);

        WowTalents mockTalents = mock(WowTalents.class);
        Optional<WowTalents> talents = Optional.of(mockTalents);

        when(characterRepository.findById(eq(charPk))).thenReturn(Optional.ofNullable(mockEntity));
        when(profileMapper.mapToDomain(mockEntity)).thenReturn(mockDomain);
        when(specService.getTalentsForCharacter(eq(realmSlug), eq(characterName))).thenReturn(talents);
        when(equipmentService.getEquipmentForCharacter(mockEntity)).thenReturn(mockEquipment);

        // when
        CharacterInfo characterInfo = orchestrator.getCharacterInfo(charId, realmSlug, blizzId);

        // then
        assertThat(characterInfo.wowCharacter()).isEqualTo(mockDomain);
        assertThat(characterInfo.equipment()).isEqualTo(mockEquipment);
        assertThat(characterInfo.talents()).isEqualTo(talents);

        verify(characterRepository).findById(eq(charPk));
        verify(profileMapper).mapToDomain(mockEntity);
        verify(specService).getTalentsForCharacter(eq(realmSlug), eq(characterName));
        verify(equipmentService).getEquipmentForCharacter(mockEntity);
        verify(eventPublisher).publishEvent(any(CharacterSelectedEvent.class));
    }
}
