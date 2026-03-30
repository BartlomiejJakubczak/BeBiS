package com.bebis.BeBiS.profile.jpa;

import com.bebis.BeBiS.base.BaseDatabaseTest;
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
import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop"
}) // to be replaced by a migration tool like Flyway or Liquibase
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // replace H2 from DataJpaTest with postgre
public class WowCharacterRepositoryTest extends BaseDatabaseTest {

    @Autowired
    private WowCharacterRepository wowCharacterRepository;

    private final ProfileMapper profileMapper = new ProfileMapper();

    @Test
    void shouldReturnAllCharactersByBlizzardId() {
        // given
        WowAccountDTO account1 = ProfileTestData.generateWowAccountDTOList(1, 3).getFirst();
        WowAccountDTO account2 = ProfileTestData.generateWowAccountDTOList(1, 1).getFirst();
        ProfileSummaryResponse profile1 = ProfileTestData.generateProfileSummaryResponse(List.of(account1));
        ProfileSummaryResponse profile2 = ProfileTestData.generateProfileSummaryResponse(List.of(account2));
        List<WowCharacterEntity> savedEntities = wowCharacterRepository.saveAll(profileMapper.mapToEntity(profile1, profile1.blizzardAccountId()));
        wowCharacterRepository.saveAll(profileMapper.mapToEntity(profile2, profile2.blizzardAccountId()));
        // when
        long queriedBlizzardAccountId = profile1.blizzardAccountId();
        List<WowCharacterEntity> charactersFromRepo = wowCharacterRepository.findAllByPk_BlizzardAccountId(queriedBlizzardAccountId);
        // then
        assertThat(charactersFromRepo)
                .hasSize(savedEntities.size())
                .containsExactlyInAnyOrderElementsOf(savedEntities); // assertEquals maintains order, so assertThat is more ideal for db testing
    }

    @Test
    void shouldUpdateEntityWithSameCompositeKey() {
        // given
        WowCharacterEntity.CompositeKey compositeKey = new WowCharacterEntity.CompositeKey(2137, "soulseeker", 1);
        WowCharacterEntity entity = profileMapper.fromDTO(ProfileTestData.generateWowCharacterDTO(compositeKey.getId(), "Thelemar", compositeKey.getRealmSlug()), compositeKey.getBlizzardAccountId());
        wowCharacterRepository.save(entity);
        WowCharacterEntity duplicate = profileMapper.fromDTO(ProfileTestData.generateWowCharacterDTO(compositeKey.getId(), "Kraghul", compositeKey.getRealmSlug()), compositeKey.getBlizzardAccountId());
        // when
        wowCharacterRepository.save(duplicate);
        // then
        assertEquals("Kraghul", wowCharacterRepository.findById(entity.getPk()).get().getName());
    }

    @Test
    void shouldSaveEntityWithTheSameIdAndSlugButDifferentBlizzardAccounts() {
        // given
        long characterId = 2137;
        String realmSlug = "soulseeker";
        WowCharacterEntity entity1 = profileMapper.fromDTO(ProfileTestData.generateWowCharacterDTO(characterId, "Thelemar", realmSlug), 1);
        WowCharacterEntity entity2 = profileMapper.fromDTO(ProfileTestData.generateWowCharacterDTO(characterId, "Kraghul", realmSlug), 2);
        // when
        wowCharacterRepository.save(entity1);
        wowCharacterRepository.save(entity2);
        // then
        assertThat(wowCharacterRepository.findAll()).hasSize(2);
    }
}
