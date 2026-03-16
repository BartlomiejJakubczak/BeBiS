package com.bebis.BeBiS.profile;

import com.bebis.BeBiS.integration.blizzard.BlizzardUserClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
        String expectedSummary = "Profile Summary";
        // when
        when(blizzardClient.getProfileSummary()).thenReturn(expectedSummary);
        String response = profileService.getProfileSummary();
        // then
        assertEquals(expectedSummary, response);
        verify(blizzardClient).getProfileSummary();
    }

}
