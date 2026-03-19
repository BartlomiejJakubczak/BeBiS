package com.bebis.BeBiS.profile;

import com.bebis.BeBiS.integration.blizzard.BlizzardUserClient;
import com.bebis.BeBiS.integration.blizzard.dto.ProfileSummaryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class ProfileServiceTest {

    @Mock
    private BlizzardUserClient blizzardClient;

    private final ProfileMapper profileMapper = new ProfileMapper();

    private ProfileService profileService;

    @BeforeEach
    void setUp() {
        profileService = new ProfileService(blizzardClient, profileMapper);
    }

    @Test
    void shouldGetListOfCharactersFromProfileSummary() {
        // given
        ProfileSummaryResponse expectedSummary = ProfileTestData.generateProfileSummaryResponse();
        List<WowCharacter> allCharactersFromSummary = profileMapper.mapToDomain(expectedSummary);
        // when
        when(blizzardClient.getProfileSummary()).thenReturn(expectedSummary);
        List<WowCharacter> allCharacters = profileService.getProfileSummary();
        // then
        assertEquals(allCharactersFromSummary, allCharacters);
        verify(blizzardClient).getProfileSummary();
    }

}
