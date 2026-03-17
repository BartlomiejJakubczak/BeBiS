package com.bebis.BeBiS.profile;

import com.bebis.BeBiS.integration.blizzard.BlizzardUserClient;
import com.bebis.BeBiS.integration.blizzard.dto.ProfileSummaryResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class ProfileServiceTest {

    @Mock
    private BlizzardUserClient blizzardClient;

    @InjectMocks
    private ProfileService profileService;

    @Test
    void shouldGetProfileSummaryFromBlizzard() {
        // given
        ProfileSummaryResponse expectedSummary = new ProfileSummaryResponse(12345, new ArrayList<>());
        // when
        when(blizzardClient.getProfileSummary()).thenReturn(expectedSummary);
        ProfileSummaryResponse response = profileService.getProfileSummary();
        // then
        assertEquals(expectedSummary, response);
        verify(blizzardClient).getProfileSummary();
    }

}
