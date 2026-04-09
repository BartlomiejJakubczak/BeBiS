package com.bebis.BeBiS.profile.jpa;

import com.bebis.BeBiS.profile.WowCharacter;
import com.bebis.BeBiS.profile.dto.CharacterSyncData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class WowCharacterEntityFactoryTest {

    private final WowCharacterEntityFactory factory = new WowCharacterEntityFactory();

    @Test
    void shouldCreateNewCharacterWithLinkedEquipmentAndCorrectData() {
        // given
        CharacterSyncData syncData = new CharacterSyncData(
                2137L, "soulseeker", 1L, "Thelamar", 60,
                WowCharacter.Race.UNDEAD, WowCharacter.WowClass.MAGE, "Soulseeker"
        );

        // when
        WowCharacterEntity result = factory.createNewCharacter(syncData);

        // then: 1. Identity Check
        assertThat(result.getPk()).isNotNull();
        assertThat(result.getPk().getId()).isEqualTo(2137L);
        assertThat(result.getPk().getRealmSlug()).isEqualTo("soulseeker");
        assertThat(result.getPk().getBlizzardAccountId()).isEqualTo(1L);

        // then: 2. Bidirectional Relationship Check
        assertThat(result.getEquipment()).isNotNull();
        assertThat(result.getEquipment().getCharacter())
                .as("The Equipment must point back to the Character for JPA bidirectional mapping")
                .isEqualTo(result);

        // then: 3. Population Check
        assertThat(result.getName()).isEqualTo("Thelamar");
        assertThat(result.getWowClass()).isEqualTo(WowCharacter.WowClass.MAGE);
    }

    @Test
    void shouldCreateBulkCharactersFromList() {
        // given
        List<CharacterSyncData> syncList = List.of(
                createDummySyncData(1L, "char1"),
                createDummySyncData(2L, "char2")
        );

        // when
        List<WowCharacterEntity> results = factory.createNewCharacters(syncList);

        // then
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getName()).isEqualTo("char1");
        assertThat(results.get(1).getName()).isEqualTo("char2");
    }

    private CharacterSyncData createDummySyncData(long id, String name) {
        return new CharacterSyncData(
                id, "realm", 1L, name, 60,
                WowCharacter.Race.HUMAN, WowCharacter.WowClass.WARRIOR, "Realm"
        );
    }

}
