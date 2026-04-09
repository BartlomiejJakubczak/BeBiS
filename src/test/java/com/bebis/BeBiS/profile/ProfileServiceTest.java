package com.bebis.BeBiS.profile;

import com.bebis.BeBiS.integration.blizzard.BlizzardUserClient;
import com.bebis.BeBiS.integration.blizzard.dto.ProfileSummaryResponse;
import com.bebis.BeBiS.profile.jpa.WowCharacterEntity;
import com.bebis.BeBiS.profile.jpa.WowCharacterEntityFactory;
import com.bebis.BeBiS.profile.jpa.WowCharacterRepository;
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
    private WowCharacterRepository wowCharacterRepository;

    private final ProfileMapper profileMapper = new ProfileMapper();

    private final WowCharacterEntityFactory characterEntityFactory = new WowCharacterEntityFactory();

    private ProfileService profileService;

    @BeforeEach
    void setUp() {
        profileService = new ProfileService(blizzardClient, wowCharacterRepository, profileMapper, characterEntityFactory);
    }

    @Test
    void shouldGetCharactersFromProfileSummaryRepoInSync() {
        // given
        ProfileSummaryResponse expectedSummary = ProfileTestData.generateProfileSummaryResponse();
        long blizzardAccountId = 1L;
        List<WowCharacterEntity> summaryEntities = characterEntityFactory.createNewCharacters(profileMapper.mapToSyncData(expectedSummary, blizzardAccountId));
        when(blizzardClient.getProfileSummary()).thenReturn(expectedSummary);
        when(wowCharacterRepository.findAllByPk_BlizzardAccountId(blizzardAccountId)).thenReturn(summaryEntities);
        // when
        profileService.getProfileSummary(blizzardAccountId);
        // then
        verify(blizzardClient).getProfileSummary();
        // no update, save or delete, so invoke repo 1 time
        verify(wowCharacterRepository, times(1)).findAllByPk_BlizzardAccountId(blizzardAccountId);
        verify(wowCharacterRepository, never()).saveAll(any());
        verify(wowCharacterRepository, never()).deleteAll(any());
    }

    @Test
    void shouldGetCharactersFromProfileSummaryWithFewerCharacters() {
        // given
        ProfileSummaryResponse summary = ProfileTestData.generateProfileSummaryResponse();
        long blizzardAccountId = 1L;
        List<WowCharacterEntity> repoEntities = new ArrayList<>(characterEntityFactory.createNewCharacters(profileMapper.mapToSyncData(summary, blizzardAccountId)));
        WowCharacterEntity oldEntity = characterEntityFactory.createNewCharacter(profileMapper.fromDTO(ProfileTestData.generateWowCharacterDTO(2137), blizzardAccountId));
        repoEntities.add(oldEntity);
        when(blizzardClient.getProfileSummary()).thenReturn(summary);
        when(wowCharacterRepository.findAllByPk_BlizzardAccountId(blizzardAccountId)).thenReturn(repoEntities);
        // when
        profileService.getProfileSummary(blizzardAccountId);
        // then
        verify(blizzardClient).getProfileSummary();
        // deletion happened, so re-sync repo after
        verify(wowCharacterRepository, times(2)).findAllByPk_BlizzardAccountId(blizzardAccountId);
        verify(wowCharacterRepository, never()).saveAll(any());
        verify(wowCharacterRepository, times(1)).deleteAll(List.of(oldEntity));
    }

    @Test
    void shouldGetCharactersFromProfileSummaryWithMoreCharacters() {
        // given
        ProfileSummaryResponse summary = ProfileTestData.generateProfileSummaryResponse();
        long blizzardAccountId = 1L;
        List<WowCharacterEntity> repoEntities = new ArrayList<>(characterEntityFactory.createNewCharacters(profileMapper.mapToSyncData(summary, blizzardAccountId)));
        WowCharacterEntity toAdd = repoEntities.removeFirst();
        when(blizzardClient.getProfileSummary()).thenReturn(summary);
        when(wowCharacterRepository.findAllByPk_BlizzardAccountId(blizzardAccountId)).thenReturn(repoEntities);
        // when
        profileService.getProfileSummary(blizzardAccountId);
        // then
        verify(blizzardClient).getProfileSummary();
        // new character in summary, saveAll and re-sync
        verify(wowCharacterRepository, times(2)).findAllByPk_BlizzardAccountId(blizzardAccountId);
        verify(wowCharacterRepository, never()).deleteAll(any());
        verify(wowCharacterRepository, times(1)).saveAll(List.of(toAdd));
    }

    @Test
    void shouldGetCharactersFromProfileSummaryWithEditedCharacters() {
        // given
        ProfileSummaryResponse summary = ProfileTestData.generateProfileSummaryResponse();
        long blizzardAccountId = 1L;
        List<WowCharacterEntity> repoEntities = new ArrayList<>(characterEntityFactory.createNewCharacters(profileMapper.mapToSyncData(summary, blizzardAccountId)));
        repoEntities.getFirst().setName("Edited");
        when(blizzardClient.getProfileSummary()).thenReturn(summary);
        when(wowCharacterRepository.findAllByPk_BlizzardAccountId(blizzardAccountId)).thenReturn(repoEntities);
        // when
        profileService.getProfileSummary(blizzardAccountId);
        // then
        // edited character in summary, still a change so re-sync
        verify(wowCharacterRepository, times(2)).findAllByPk_BlizzardAccountId(blizzardAccountId);
        verify(wowCharacterRepository, never()).deleteAll(any());
        verify(wowCharacterRepository, never()).saveAll(any());
    }
}
