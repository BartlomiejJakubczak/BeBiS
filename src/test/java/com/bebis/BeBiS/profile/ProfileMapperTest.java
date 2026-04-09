package com.bebis.BeBiS.profile;

import com.bebis.BeBiS.equipment.jpa.EquipmentEntity;
import com.bebis.BeBiS.integration.blizzard.dto.ProfileSummaryResponse;
import com.bebis.BeBiS.integration.blizzard.dto.WowAccountDTO;
import com.bebis.BeBiS.integration.blizzard.dto.WowCharacterDTO;
import com.bebis.BeBiS.profile.dto.CharacterSyncData;
import com.bebis.BeBiS.profile.jpa.WowCharacterEntity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ProfileMapperTest {

    private final ProfileMapper profileMapper = new ProfileMapper();

    // mapToSyncData

    @Test
    void shouldReturnEmptyListFromNoAccounts() {
        //given
        ProfileSummaryResponse profileSummaryResponse = ProfileTestData.generateProfileSummaryResponse(null, null);
        long blizzardAccountId = 1L;
        //then
        assertEquals(List.of(), profileMapper.mapToSyncData(profileSummaryResponse, blizzardAccountId));
    }

    @Test
    void shouldReturnEmptyListFromAccountWithNoCharacters() {
        //given
        ProfileSummaryResponse profileSummaryResponse = ProfileTestData.generateProfileSummaryResponse(1, 0);
        long blizzardAccountId = 1L;
        //then
        assertEquals(List.of(), profileMapper.mapToSyncData(profileSummaryResponse, blizzardAccountId));
    }

    @Test
    void shouldReturnEmptyListFromMultipleAccountsWithNoCharacters() {
        //given
        ProfileSummaryResponse profileSummaryResponse = ProfileTestData.generateProfileSummaryResponse(2, 0);
        long blizzardAccountId = 1L;
        //then
        assertEquals(List.of(), profileMapper.mapToSyncData(profileSummaryResponse, blizzardAccountId));
    }

    @Test
    void shouldReturnCharacterEntityFromASingleAccount() {
        // given
        String characterName = "Thelamar";
        String realmName = "Soulseeker";
        int characterLevel = 60;
        int characterId = 1;
        long blizzardAccountId = 1;
        WowCharacterDTO characterDto = ProfileTestData.generateWowCharacterDTO(1, characterName, realmName);
        WowAccountDTO accountDto = new WowAccountDTO(1, List.of(characterDto));
        ProfileSummaryResponse response = new ProfileSummaryResponse(List.of(accountDto));
        // when
        List<CharacterSyncData> result = profileMapper.mapToSyncData(response, blizzardAccountId);
        // then
        assertEquals(1, result.size());
        CharacterSyncData mapped = result.getFirst();
        assertEquals(characterId, mapped.characterId());
        assertEquals(realmName.toLowerCase(), mapped.realmSlug());
        assertEquals(blizzardAccountId, mapped.blizzardAccountId());
        assertEquals(characterName, mapped.name());
        assertEquals(characterDto.realm().name(), mapped.realmName());
        assertEquals(characterLevel, mapped.level());
        assertEquals(characterDto.race().name(), mapped.race().name());
        assertEquals(characterDto.wowClass().name(), mapped.wowClass().name());
    }

    @Test
    void shouldReturnCharacterEntitiesFromMultipleAccounts() {
        // given - Account 1 with 2 characters
        WowCharacterDTO char1 = ProfileTestData.generateWowCharacterDTO(1, "Thelamar", "Soulseeker");
        WowCharacterDTO char2 = ProfileTestData.generateWowCharacterDTO(2, "Leeroy", "Soulseeker");
        WowAccountDTO account1 = new WowAccountDTO(1, List.of(char1, char2));

        // Account 2 with 1 character
        WowCharacterDTO char3 = ProfileTestData.generateWowCharacterDTO(3, "Thrall", "Soulseeker");
        WowAccountDTO account2 = new WowAccountDTO(2, List.of(char3));

        long blizzardAccountId = 1L;
        ProfileSummaryResponse response = new ProfileSummaryResponse(List.of(account1, account2));

        // when
        List<CharacterSyncData> result = profileMapper.mapToSyncData(response, blizzardAccountId);

        // then
        assertEquals(3, result.size(), "Should have exactly 3 characters in total");

        // Verify specific "Anchor Points" to ensure coverage
        assertEquals("Thelamar", result.get(0).name(), "First character of first account missing");
        assertEquals("Leeroy", result.get(1).name(), "Second character of first account missing");
        assertEquals("Thrall", result.get(2).name(), "Character from second account missing");

        // Verify a domain field to ensure the mapping was complete
        assertEquals(char3.race().name(), result.get(2).race().name(), "Race from second account missing");
    }

    // mapToDomain

    @Test
    void shouldReturnDomainCharacterFromEntity() {
        // given
        long characterId = 12345L;
        long blizzardAccountId = 1L;
        String realmSlug = "soulseeker";

        WowCharacterEntity entity = new WowCharacterEntity(
                new WowCharacterEntity.CompositeKey(characterId, realmSlug, blizzardAccountId),
                new EquipmentEntity(),
                "Thelemar",
                60,
                WowCharacter.Race.NIGHT_ELF,
                WowCharacter.WowClass.ROGUE,
                "Soulseeker"
        );

        // when
        WowCharacter result = profileMapper.mapToDomain(entity);

        // then
        assertEquals(characterId, result.wowCharacterId().id(), "Domain ID must match Blizzard ID");
        assertEquals(realmSlug, result.wowCharacterId().realmSlug());
        assertEquals("Thelemar", result.name());
        assertEquals(60, result.level());
        assertEquals(WowCharacter.Race.NIGHT_ELF, result.race());
        assertEquals(WowCharacter.WowClass.ROGUE, result.wowClass());
        assertEquals("Soulseeker", result.realm().name());

        // Ensure the 'profileId' (blizzardAccountId) DID NOT leak into the domain
        // If your WowCharacter record doesn't have a profileId field,
        // this is implicitly tested by the code even compiling.
    }

    @Test
    void shouldReturnDomainCharactersFromList() {
        // given
        long blizzardAccountId = 1L; // The "Owner"
        WowCharacterEntity entity1 = new WowCharacterEntity(
                new WowCharacterEntity.CompositeKey(1L, "soulseeker", blizzardAccountId),
                new EquipmentEntity(), "Char1", 60, WowCharacter.Race.ORC, WowCharacter.WowClass.WARRIOR, "Soulseeker"
        );
        WowCharacterEntity entity2 = new WowCharacterEntity(
                new WowCharacterEntity.CompositeKey(2L, "soulseeker", blizzardAccountId),
                new EquipmentEntity(), "Char2", 60, WowCharacter.Race.TROLL, WowCharacter.WowClass.MAGE, "Soulseeker"
        );
        List<WowCharacterEntity> entities = List.of(entity1, entity2);

        // when
        List<WowCharacter> result = profileMapper.mapToDomain(entities);

        // then
        assertEquals(2, result.size());
        assertEquals("Char1", result.get(0).name());
        assertEquals("Char2", result.get(1).name());
    }

    @Test
    void shouldReturnEmptyListWhenMappingEmptyEntityList() {
        // when
        List<WowCharacter> result = profileMapper.mapToDomain(List.of());

        // then
        assertEquals(List.of(), result, "Mapping an empty list should return an empty list, not null");
    }

}
