package com.bebis.BeBiS.profile;

import com.bebis.BeBiS.integration.blizzard.BlizzardUserClient;
import com.bebis.BeBiS.integration.blizzard.dto.ProfileSummaryResponse;
import com.bebis.BeBiS.profile.dto.CharacterSyncData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
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
        ProfileSummaryResponse expectedSummary = ProfileTestData.generateProfileSummaryResponse();
        long blizzardAccountId = 1L;
        List<CharacterSyncData> syncData = new ArrayList<>();

        when(blizzardClient.getProfileSummary()).thenReturn(expectedSummary);
        when(profileMapper.mapToSyncData(expectedSummary, blizzardAccountId)).thenReturn(syncData);
        when(synchronizer.synchronize(syncData, blizzardAccountId)).thenReturn(List.of());

        // when
        profileService.getProfileSummary(blizzardAccountId);

        // then
        verify(blizzardClient, times(1)).getProfileSummary();
        verify(synchronizer, times(1)).synchronize(syncData, blizzardAccountId);
        verify(profileMapper, times(1)).mapToDomain(anyList());
    }

}
