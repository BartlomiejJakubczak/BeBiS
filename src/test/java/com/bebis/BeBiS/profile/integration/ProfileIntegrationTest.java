package com.bebis.BeBiS.profile.integration;

import com.bebis.BeBiS.base.BaseFullStackTest;
import com.bebis.BeBiS.equipment.jpa.EquipmentEntity;
import com.bebis.BeBiS.integration.blizzard.BlizzardUserClient;
import com.bebis.BeBiS.integration.blizzard.dto.ProfileSummaryResponse;
import com.bebis.BeBiS.integration.blizzard.dto.WowAccountDTO;
import com.bebis.BeBiS.integration.blizzard.dto.WowCharacterDTO;
import com.bebis.BeBiS.profile.ProfileService;
import com.bebis.BeBiS.profile.ProfileTestData;
import com.bebis.BeBiS.profile.WowCharacter;
import com.bebis.BeBiS.profile.jpa.WowCharacterEntity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class ProfileIntegrationTest extends BaseFullStackTest {

    @Autowired
    private ProfileService service;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private BlizzardUserClient blizzardUserClient;

    private static final long BLIZZ_ACCOUNT_ID = 1;

    @Test
    void shouldPersistNewCharacterEntityWithEmptyEquipment() {
        // given
        ProfileSummaryResponse response = ProfileTestData.generateProfileSummaryResponse(1, 1);
        when(blizzardUserClient.getProfileSummary()).thenReturn(response);

        List<WowCharacterDTO> dtos = extractCharactersFromSummary(response);
        WowCharacterDTO dto = dtos.getFirst();

        WowCharacterEntity.CompositeKey pk = new WowCharacterEntity.CompositeKey(dto.wowCharacterId(), dto.realm().slug(), BLIZZ_ACCOUNT_ID);

        //when
        callService();

        // then
        WowCharacterEntity entity = entityManager.find(WowCharacterEntity.class, pk);
        assertThat(entity).isNotNull();
        assertThat(entity.getEquipment()).isNotNull();

        Integer equipmentCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM character_equipment WHERE character_id = ? AND realm_slug = ?",
                Integer.class, dto.wowCharacterId(), dto.realm().slug()
        );
        assertThat(equipmentCount).isEqualTo(1);
    }

    @Test
    void shouldDeleteOldCharacters() {
        // given
        ProfileSummaryResponse response = ProfileTestData.generateProfileSummaryResponse();
        when(blizzardUserClient.getProfileSummary()).thenReturn(response);

        List<WowCharacterDTO> dtos = new ArrayList<>(extractCharactersFromSummary(response));
        WowCharacterDTO oldDTO = putInExtraGeneratedDto(dtos);
        // manually persist the entities to db
        setUpWowCharacters(dtos);

        // when
        callService();

        // then
        List<Integer> savedIds = jdbcTemplate.queryForList("SELECT id FROM wow_characters WHERE blizzard_account_id = ?",
                Integer.class, BLIZZ_ACCOUNT_ID);
        assertThat(savedIds).doesNotContain((int) oldDTO.wowCharacterId());
    }

    @Test
    void shouldUpdateExistingCharacters() {
        // given
        ProfileSummaryResponse response = ProfileTestData.generateProfileSummaryResponse();
        when(blizzardUserClient.getProfileSummary()).thenReturn(response);

        List<WowCharacterDTO> dtos = new ArrayList<>(extractCharactersFromSummary(response));
        WowCharacterDTO first = dtos.getFirst();
        WowCharacterDTO oldDTO = ProfileTestData.generateWowCharacterDTO(first.wowCharacterId(),
                "Kraghul", "Stormreaver");
        dtos.remove(first);
        dtos.add(oldDTO);
        setUpWowCharacters(dtos);

        // when
        callService();

        // then
        Map<String, Object> dbRow = jdbcTemplate.queryForMap(
                "SELECT name, realm_name FROM wow_characters WHERE id = ?",
                oldDTO.wowCharacterId());
        assertThat(dbRow.get("name")).isEqualTo(first.name());
        assertThat(dbRow.get("realm_name")).isEqualTo(first.realm().name());
    }

    private void callService() {
        service.getProfileSummary(BLIZZ_ACCOUNT_ID);
        entityManager.flush();
        entityManager.clear();
    }

    private WowCharacterDTO putInExtraGeneratedDto(List<WowCharacterDTO> dtos) {
        List<Long> ids = dtos.stream().map(WowCharacterDTO::wowCharacterId).toList();
        Random rand = new Random();
        long newId = rand.nextLong();
        while (ids.contains(newId)) {
            newId = rand.nextLong();
        }
        WowCharacterDTO generatedDTO = ProfileTestData.generateWowCharacterDTO((int) newId);
        dtos.add(generatedDTO);
        return generatedDTO;
    }

    private void setUpWowCharacters(List<WowCharacterDTO> dtos) {
        List<WowCharacterEntity> toPersist = new ArrayList<>();
        for (WowCharacterDTO dto : dtos) {
            WowCharacterEntity entity = new WowCharacterEntity();
            entity.setPk(new WowCharacterEntity.CompositeKey(dto.wowCharacterId(), dto.realm().slug(), BLIZZ_ACCOUNT_ID));
            EquipmentEntity eqEntity = new EquipmentEntity();
            eqEntity.setCharacter(entity);
            entity.setEquipment(eqEntity);
            entity.setName(dto.name());
            entity.setLevel(dto.level());
            entity.setRace(WowCharacter.Race.fromBlizzardName(dto.race().name()));
            entity.setWowClass(WowCharacter.WowClass.valueOf(dto.wowClass().name().toUpperCase()));
            entity.setRealmName(dto.realm().name());
            toPersist.add(entity);
        }
        toPersist.forEach(entityManager::persist);
        entityManager.flush();
        entityManager.clear();
    }

    private List<WowCharacterDTO> extractCharactersFromSummary(ProfileSummaryResponse response) {
        return response.wowAccounts().stream()
                .map(WowAccountDTO::characters)
                .flatMap(List::stream)
                .toList();
    }

}
