package com.bebis.BeBiS.profile;

import com.bebis.BeBiS.integration.blizzard.dto.ProfileSummaryResponse;
import com.bebis.BeBiS.integration.blizzard.dto.WowAccountDTO;
import com.bebis.BeBiS.integration.blizzard.dto.WowCharacterDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ProfileMapperTest {

    private final ProfileMapper profileMapper = new ProfileMapper();

    @Test
    void shouldReturnEmptyListFromNoAccounts() {
        //given
        ProfileSummaryResponse profileSummaryResponse = ProfileTestData.generateProfileSummaryResponse(null, null);
        //then
        assertEquals(List.of(), profileMapper.mapToDomain(profileSummaryResponse));
    }

    @Test
    void shouldReturnEmptyListFromAccountWithNoCharacters() {
        //given
        ProfileSummaryResponse profileSummaryResponse = ProfileTestData.generateProfileSummaryResponse(1, 0);
        //then
        assertEquals(List.of(), profileMapper.mapToDomain(profileSummaryResponse));
    }

    @Test
    void shouldReturnEmptyListFromMultipleAccountsWithNoCharacters() {
        //given
        ProfileSummaryResponse profileSummaryResponse = ProfileTestData.generateProfileSummaryResponse(2, 0);
        //then
        assertEquals(List.of(), profileMapper.mapToDomain(profileSummaryResponse));
    }

    @Test
    void shouldReturnCharacterFromASingleAccount() {
        // given
        String characterName = "Thelamar";
        int characterLevel = 60;
        int characterId = 1;
        WowCharacterDTO characterDto = ProfileTestData.generateWowCharacterDTO(1, characterName, characterLevel);
        WowAccountDTO accountDto = new WowAccountDTO(1, List.of(characterDto));
        ProfileSummaryResponse response = new ProfileSummaryResponse(0, List.of(accountDto));
        // when
        List<WowCharacter> result = profileMapper.mapToDomain(response);
        // then
        assertEquals(1, result.size());
        WowCharacter mapped = result.getFirst();
        assertEquals(characterId, mapped.id());
        assertEquals(characterName, mapped.name());
        assertEquals(characterDto.realm().name(), mapped.realm().name());
        assertEquals(characterLevel, mapped.level());
        assertEquals(characterDto.race().name(), mapped.race().name());
        assertEquals(characterDto.wowClass().name(), mapped.wowClass().name());
    }

    @Test
    void shouldReturnCharactersFromMultipleAccounts() {
        // given - Account 1 with 2 characters
        WowCharacterDTO char1 = ProfileTestData.generateWowCharacterDTO(1, "Thelamar", 60);
        WowCharacterDTO char2 = ProfileTestData.generateWowCharacterDTO(2, "Leeroy", 60);
        WowAccountDTO account1 = new WowAccountDTO(1, List.of(char1, char2));

        // Account 2 with 1 character
        WowCharacterDTO char3 = ProfileTestData.generateWowCharacterDTO(3, "Thrall", 60);
        WowAccountDTO account2 = new WowAccountDTO(2, List.of(char3));

        ProfileSummaryResponse response = new ProfileSummaryResponse(0, List.of(account1, account2));

        // when
        List<WowCharacter> result = profileMapper.mapToDomain(response);

        // then
        assertEquals(3, result.size(), "Should have exactly 3 characters in total");

        // Verify specific "Anchor Points" to ensure coverage
        assertEquals("Thelamar", result.get(0).name(), "First character of first account missing");
        assertEquals("Leeroy", result.get(1).name(), "Second character of first account missing");
        assertEquals("Thrall", result.get(2).name(), "Character from second account missing");

        // Verify a domain field to ensure the mapping was complete
        assertEquals(char3.race().name(), result.get(2).race().name(), "Race from second account missing");
    }

}
