package com.bebis.BeBiS.profile;

import com.bebis.BeBiS.profile.dto.CharacterSyncData;
import com.bebis.BeBiS.profile.jpa.WowCharacterEntity;
import com.bebis.BeBiS.profile.jpa.WowCharacterEntityFactory;
import com.bebis.BeBiS.profile.jpa.WowCharacterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProfileSynchronizerTest {

    @Mock
    private WowCharacterRepository characterRepository;

    @Mock
    private WowCharacterEntityFactory characterEntityFactory;

    @Captor
    private ArgumentCaptor<List<WowCharacterEntity>> deleteCaptor;

    @Captor
    private ArgumentCaptor<List<WowCharacterEntity>> saveCaptor;

    private ProfileSynchronizer synchronizer;

    @BeforeEach
    void setUp() {
        synchronizer = new ProfileSynchronizer(characterRepository, characterEntityFactory);
    }

    @Test
    void shouldReturnExistingWhenInSync() {
        // given
        long blizzardAccountId = 1L;
        CharacterSyncData fresh = createSyncData(100, "Thelamar");
        WowCharacterEntity existing = createEntity(100, "Thelamar");

        when(characterRepository.findAllByPk_BlizzardAccountId(blizzardAccountId)).thenReturn(List.of(existing));

        // when
        synchronizer.synchronize(List.of(fresh), blizzardAccountId);
        // then
        // no update, save or delete, so invoke repo 1 time
        verify(characterRepository, times(1)).findAllByPk_BlizzardAccountId(blizzardAccountId);
        verify(characterRepository, never()).saveAll(any());
        verify(characterRepository, never()).deleteAll(any());
    }

    @Test
    void shouldDeleteOrphanedCharacters() {
        // given
        long blizzardAccountId = 1L;
        WowCharacterEntity toDelete = createEntity(666, "OldHero");

        when(characterRepository.findAllByPk_BlizzardAccountId(blizzardAccountId)).thenReturn(List.of(toDelete));
        // Blizzard returns nothing
        List<CharacterSyncData> emptyFromBlizzard = Collections.emptyList();

        // when
        synchronizer.synchronize(emptyFromBlizzard, blizzardAccountId);

        // then
        verify(characterRepository).deleteAll(deleteCaptor.capture());
        assertThat(deleteCaptor.getValue()).extracting(e -> e.getPk().getId()).containsExactly(666L);
        verify(characterRepository, times(2)).findAllByPk_BlizzardAccountId(blizzardAccountId);
    }

    @Test
    void shouldSaveNewCharactersUsingFactory() {
        // given
        long blizzardAccountId = 1L;
        CharacterSyncData newCharData = createSyncData(777, "Newbie");
        WowCharacterEntity newEntity = createEntity(777, "Newbie");

        when(characterRepository.findAllByPk_BlizzardAccountId(blizzardAccountId)).thenReturn(List.of());
        when(characterEntityFactory.createNewCharacter(newCharData)).thenReturn(newEntity);

        // when
        synchronizer.synchronize(List.of(newCharData), blizzardAccountId);

        // then
        verify(characterRepository).saveAll(saveCaptor.capture());
        assertThat(saveCaptor.getValue()).containsExactly(newEntity);
        verify(characterRepository, times(2)).findAllByPk_BlizzardAccountId(blizzardAccountId);
    }

    @Test
    void shouldTriggerReSyncOnUpdate() {
        // given
        long blizzardAccountId = 1L;
        WowCharacterEntity existing = spy(createEntity(100, "OldName"));
        CharacterSyncData fresh = createSyncData(100, "NewName");

        when(characterRepository.findAllByPk_BlizzardAccountId(blizzardAccountId)).thenReturn(List.of(existing));

        // when
        synchronizer.synchronize(List.of(fresh), blizzardAccountId);

        // then: No save/delete needed (dirty checking), but re-sync triggered
        verify(characterRepository, never()).saveAll(any());
        verify(characterRepository, times(2)).findAllByPk_BlizzardAccountId(blizzardAccountId);
    }

    private CharacterSyncData createSyncData(long id, String name) {
        return new CharacterSyncData(
                id,
                "soulseeker",
                1L,
                name,
                60,
                WowCharacter.Race.ORC,
                WowCharacter.WowClass.ROGUE,
                "Soulseeker"
        );
    }

    private WowCharacterEntity createEntity(long id, String name) {
        WowCharacterEntity entity = new WowCharacterEntity();
        entity.setPk(new WowCharacterEntity.CompositeKey(id, "soulseeker", 1L));
        entity.setName(name);
        entity.setLevel(60);
        entity.setRace(WowCharacter.Race.ORC);
        entity.setWowClass(WowCharacter.WowClass.ROGUE);
        entity.setRealmName("Soulseeker");
        return entity;
    }

}
