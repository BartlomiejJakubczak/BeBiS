package com.bebis.BeBiS.profile.jpa;

import com.bebis.BeBiS.base.BasePersistenceTest;
import com.bebis.BeBiS.integration.blizzard.dto.ProfileSummaryResponse;
import com.bebis.BeBiS.integration.blizzard.dto.WowAccountDTO;
import com.bebis.BeBiS.profile.ProfileMapper;
import com.bebis.BeBiS.profile.ProfileTestData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@TestPropertySource(properties = {"spring.jpa.hibernate.ddl-auto=validate"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // replace H2 from DataJpaTest with postgre
public class WowCharacterRepositoryTest extends BasePersistenceTest {

    @Autowired
    private WowCharacterRepository characterRepository;

    private final ProfileMapper profileMapper = new ProfileMapper();

    private final WowCharacterEntityFactory characterEntityFactory = new WowCharacterEntityFactory();

    @Test
    void shouldReturnAllCharactersByBlizzardId() {
        // given
        WowAccountDTO account1 = ProfileTestData.generateWowAccountDTOList(1, 3).getFirst();
        WowAccountDTO account2 = ProfileTestData.generateWowAccountDTOList(1, 1).getFirst();
        ProfileSummaryResponse profile1 = ProfileTestData.generateProfileSummaryResponse(List.of(account1));
        ProfileSummaryResponse profile2 = ProfileTestData.generateProfileSummaryResponse(List.of(account2));
        long queriedBlizzardAccountId = 1L;
        long differentBlizzardAccountId = 2L;
        List<WowCharacterEntity> savedEntities = characterRepository.saveAll(
                characterEntityFactory.createNewCharacters(profileMapper.mapToSyncData(profile1, queriedBlizzardAccountId))
        );
        characterRepository.saveAll(
                characterEntityFactory.createNewCharacters(profileMapper.mapToSyncData(profile2, differentBlizzardAccountId))
        );
        // when
        List<WowCharacterEntity> charactersFromRepo = characterRepository.findAllByPk_BlizzardAccountId(queriedBlizzardAccountId);
        // then
        assertThat(charactersFromRepo)
                .hasSize(savedEntities.size())
                .containsExactlyInAnyOrderElementsOf(savedEntities); // assertEquals maintains order, so assertThat is more ideal for db testing
    }

    @Test
    void shouldIdentifyExistingEntityByCompositeKeyDuringManualUpdate() {
        // given
        WowCharacterEntity.CompositeKey pk = new WowCharacterEntity.CompositeKey(2137, "soulseeker", 1);
        WowCharacterEntity initial = characterEntityFactory.createNewCharacter(
                profileMapper.fromDTO(ProfileTestData.generateWowCharacterDTO(pk.getId(), "Thelamar", pk.getRealmSlug()), pk.getBlizzardAccountId())
        );
        characterRepository.saveAndFlush(initial); // Flush to ensure it's in the DB

        // when
        WowCharacterEntity found = characterRepository.findById(pk).orElseThrow();
        found.setName("Kraghul");
        characterRepository.saveAndFlush(found);

        // then
        assertThat(characterRepository.findById(pk)).isPresent().get()
                .extracting(WowCharacterEntity::getName)
                .isEqualTo("Kraghul");
        assertThat(characterRepository.count()).isEqualTo(1); // no new entity
    }

    @Test
    void shouldSaveEntityWithTheSameIdAndSlugButDifferentBlizzardAccounts() {
        // given
        long characterId = 2137;
        String realmSlug = "soulseeker";
        WowCharacterEntity entity1 = characterEntityFactory.createNewCharacter(
                profileMapper.fromDTO(ProfileTestData.generateWowCharacterDTO(characterId, "Thelemar", realmSlug), 1)
        );
        WowCharacterEntity entity2 = characterEntityFactory.createNewCharacter(
                profileMapper.fromDTO(ProfileTestData.generateWowCharacterDTO(characterId, "Kraghul", realmSlug), 2)
        );
        // when
        characterRepository.saveAndFlush(entity1);
        characterRepository.saveAndFlush(entity2);
        // then
        assertThat(characterRepository.findAll()).hasSize(2);
    }

    @Test
    void shouldThrowExceptionOnPrimaryKeyViolation() {
        // given
        WowCharacterEntity.CompositeKey pk = new WowCharacterEntity.CompositeKey(999, "firemaw", 1);
        WowCharacterEntity char1 = characterEntityFactory.createNewCharacter(
                profileMapper.fromDTO(ProfileTestData.generateWowCharacterDTO(pk.getId(), "A", pk.getRealmSlug()), pk.getBlizzardAccountId())
        );
        characterRepository.saveAndFlush(char1);

        // Duplicate PK
        WowCharacterEntity char2 = characterEntityFactory.createNewCharacter(
                profileMapper.fromDTO(ProfileTestData.generateWowCharacterDTO(pk.getId(), "B", pk.getRealmSlug()), pk.getBlizzardAccountId())
        );

        // when / then
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> {
            characterRepository.saveAndFlush(char2);
        });
    }
}
