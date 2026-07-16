package com.bebis.BeBiS.profile;

import com.bebis.BeBiS.integration.blizzard.BlizzardUserClient;
import com.bebis.BeBiS.integration.blizzard.dto.SpecializationResponse;
import com.bebis.BeBiS.profile.domain.WowTalents;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SpecializationServiceTest {

    @Mock
    private BlizzardUserClient client;

    @Mock
    private SpecializationMapper mapper;

    private SpecializationService service;

    @BeforeEach
    void setup() {
        service = new SpecializationService(client, mapper);
    }

    @Test
    void shouldPullAndMapSpecializationFromBlizzard() {
        // given
        String characterName = "Thelamar";
        String realmSlug = "soulseeker";

        SpecializationResponse response = mock(SpecializationResponse.class);
        WowTalents mockTalents = mock(WowTalents.class);
        Optional<WowTalents> talents = Optional.of(mockTalents);

        when(client.getCharacterSpecialization(eq(realmSlug), eq(characterName))).thenReturn(response);
        when(mapper.fromDTO(eq(response))).thenReturn(talents);

        // when
        service.getTalentsForCharacter(realmSlug, characterName);

        // then
        verify(client).getCharacterSpecialization(eq(realmSlug), eq(characterName));
        verify(mapper).fromDTO(eq(response));
    }
}
