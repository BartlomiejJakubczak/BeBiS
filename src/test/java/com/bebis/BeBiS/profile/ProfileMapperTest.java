package com.bebis.BeBiS.profile;

import com.bebis.BeBiS.equipment.jpa.EquipmentEntity;
import com.bebis.BeBiS.integration.blizzard.dto.ProfileSummaryResponse;
import com.bebis.BeBiS.integration.blizzard.dto.RaceDTO;
import com.bebis.BeBiS.integration.blizzard.dto.RealmDTO;
import com.bebis.BeBiS.integration.blizzard.dto.WowAccountDTO;
import com.bebis.BeBiS.integration.blizzard.dto.WowCharacterDTO;
import com.bebis.BeBiS.integration.blizzard.dto.WowClassDTO;
import com.bebis.BeBiS.profile.domain.WowCharacter;
import com.bebis.BeBiS.profile.domain.exception.InvalidCharacterException;
import com.bebis.BeBiS.profile.dto.CharacterSyncData;
import com.bebis.BeBiS.profile.jpa.WowCharacterEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ProfileMapperTest {

    private final ProfileMapper profileMapper = new ProfileMapper();

    // ==========================================
    // mapToDTOs Tests
    // ==========================================

    @Test
    void shouldReturnEmptyListFromNullResponse() {
        assertEquals(List.of(), profileMapper.mapToDTOs(null));
    }

    @Test
    void shouldReturnEmptyListFromNoAccounts() {
        // given
        ProfileSummaryResponse profileSummaryResponse = ProfileTestData.generateProfileSummaryResponse(null, null);

        // then
        assertEquals(List.of(), profileMapper.mapToDTOs(profileSummaryResponse));
    }

    @Test
    void shouldReturnEmptyListFromAccountWithNoCharacters() {
        // given
        ProfileSummaryResponse profileSummaryResponse = ProfileTestData.generateProfileSummaryResponse(1, 0);

        // then
        assertEquals(List.of(), profileMapper.mapToDTOs(profileSummaryResponse));
    }

    @Test
    void shouldReturnEmptyListFromMultipleAccountsWithNoCharacters() {
        // given
        ProfileSummaryResponse profileSummaryResponse = ProfileTestData.generateProfileSummaryResponse(2, 0);

        // then
        assertEquals(List.of(), profileMapper.mapToDTOs(profileSummaryResponse));
    }

    @Test
    void shouldReturnCharacterDtoFromASingleAccount() {
        // given
        String characterName = "Thelamar";
        String realmName = "Soulseeker";
        WowCharacterDTO characterDto = ProfileTestData.generateWowCharacterDTO(1, characterName, realmName);
        WowAccountDTO accountDto = new WowAccountDTO(1, List.of(characterDto));
        ProfileSummaryResponse response = new ProfileSummaryResponse(List.of(accountDto));

        // when
        List<WowCharacterDTO> result = profileMapper.mapToDTOs(response);

        // then
        assertEquals(1, result.size());
        assertEquals(characterDto, result.getFirst());
    }

    @Test
    void shouldReturnCharacterDtosFromMultipleAccounts() {
        // given - Account 1 with 2 characters
        WowCharacterDTO char1 = ProfileTestData.generateWowCharacterDTO(1, "Thelamar", "Soulseeker");
        WowCharacterDTO char2 = ProfileTestData.generateWowCharacterDTO(2, "Leeroy", "Soulseeker");
        WowAccountDTO account1 = new WowAccountDTO(1, List.of(char1, char2));

        // Account 2 with 1 character
        WowCharacterDTO char3 = ProfileTestData.generateWowCharacterDTO(3, "Thrall", "Soulseeker");
        WowAccountDTO account2 = new WowAccountDTO(2, List.of(char3));

        ProfileSummaryResponse response = new ProfileSummaryResponse(List.of(account1, account2));

        // when
        List<WowCharacterDTO> result = profileMapper.mapToDTOs(response);

        // then
        assertEquals(3, result.size(), "Should extract 3 DTOs across all accounts");
        assertEquals("Thelamar", result.get(0).name());
        assertEquals("Leeroy", result.get(1).name());
        assertEquals("Thrall", result.get(2).name());
    }

    // ==========================================
    // fromDTO Tests
    // ==========================================

    @Test
    void shouldMapDtoToCharacterSyncDataSuccessfully() {
        // given
        String characterName = "Thelamar";
        String realmName = "Soulseeker";
        long blizzardAccountId = 999L;
        WowCharacterDTO dto = ProfileTestData.generateWowCharacterDTO(1, characterName, realmName);

        // when
        CharacterSyncData mapped = profileMapper.fromDTO(dto, blizzardAccountId);

        // then
        assertNotNull(mapped);
        assertEquals(1, mapped.characterId());
        assertEquals(realmName.toLowerCase(), mapped.realmSlug());
        assertEquals(blizzardAccountId, mapped.blizzardAccountId());
        assertEquals(characterName, mapped.name());
        assertEquals(dto.realm().name(), mapped.realmName());
        assertEquals(60, mapped.level());
        assertEquals(dto.race().name(), mapped.race().name());
        assertEquals(dto.wowClass().name(), mapped.wowClass().name());
    }

    @ParameterizedTest
    @MethodSource("provideInvalidCharacterDTOs")
    @DisplayName("Should throw InvalidCharacterException when DTO invariant fields are missing or corrupted")
    void shouldThrowInvalidCharacterExceptionForCorruptDTOs(WowCharacterDTO invalidDto) {
        long blizzardAccountId = 1L;
        assertThrows(InvalidCharacterException.class, () -> profileMapper.fromDTO(invalidDto, blizzardAccountId));
    }

    private static Stream<WowCharacterDTO> provideInvalidCharacterDTOs() {
        RealmDTO validRealm = new RealmDTO(1, "Soulseeker", "soulseeker", null);
        RealmDTO realmMissingSlug = new RealmDTO(1, "Soulseeker", null, null);
        RaceDTO validRace = new RaceDTO(1, "NIGHT_ELF", null);
        RaceDTO invalidRace = new RaceDTO(1, "INVALID_RACE", null);
        WowClassDTO validClass = new WowClassDTO(1, "ROGUE", null);
        WowClassDTO invalidClass = new WowClassDTO(1, "INVALID_CLASS", null);

        return Stream.of(
                // Missing realm slug
                new WowCharacterDTO(1L, "Thelamar", 60, null, realmMissingSlug, validRace, validClass),
                // Missing race
                new WowCharacterDTO(1L, "Thelamar", 60, null, validRealm, null, validClass),
                // Unmapped/invalid race name
                new WowCharacterDTO(1L, "Thelamar", 60, null, validRealm, invalidRace, validClass),
                // Missing wowClass
                new WowCharacterDTO(1L, "Thelamar", 60, null, validRealm, validRace, null),
                // Unmapped/invalid wowClass name
                new WowCharacterDTO(1L, "Thelamar", 60, null, validRealm, validRace, invalidClass)
        );
    }

    // ==========================================
    // mapToDomain Tests
    // ==========================================

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
    }

    @Test
    void shouldReturnDomainCharactersFromList() {
        // given
        long blizzardAccountId = 1L;
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