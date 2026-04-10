package com.bebis.BeBiS.profile;

import com.bebis.BeBiS.integration.blizzard.BlizzardUserClient;
import com.bebis.BeBiS.integration.blizzard.dto.ProfileSummaryResponse;
import com.bebis.BeBiS.profile.dto.CharacterSyncData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class ProfileServiceTest {

    @Mock
    private BlizzardUserClient blizzardClient;

    @Mock
    private ProfileSynchronizer synchronizer;

    @Mock
    private ProfileMapper profileMapper;

    private ProfileService profileService;

    @BeforeEach
    void setUp() {
        profileService = new ProfileService(blizzardClient, synchronizer, profileMapper);
    }

    @Test
    void shouldPullAndSynchronizeProfileDataFromBlizzard() {
        // given
        long blizzardAccountId = 1L;

        ProfileSummaryResponse response = mock(ProfileSummaryResponse.class);
        List<CharacterSyncData> syncData = List.of(mock(CharacterSyncData.class));

        when(blizzardClient.getProfileSummary()).thenReturn(response);
        when(profileMapper.mapToSyncData(response, blizzardAccountId)).thenReturn(syncData);
        when(synchronizer.synchronize(syncData, blizzardAccountId)).thenReturn(List.of());

        // when
        profileService.getProfileSummary(blizzardAccountId);

        // then
        verify(blizzardClient).getProfileSummary();
        verify(synchronizer).synchronize(syncData, blizzardAccountId);
        verify(profileMapper).mapToDomain(anyList());
    }

}
