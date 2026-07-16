package com.bebis.BeBiS.profile;

import com.bebis.BeBiS.integration.blizzard.dto.ProfileSummaryResponse;
import com.bebis.BeBiS.integration.blizzard.dto.RaceDTO;
import com.bebis.BeBiS.integration.blizzard.dto.RealmDTO;
import com.bebis.BeBiS.integration.blizzard.dto.SpecializationResponse;
import com.bebis.BeBiS.integration.blizzard.dto.WowAccountDTO;
import com.bebis.BeBiS.integration.blizzard.dto.WowCharacterDTO;
import com.bebis.BeBiS.integration.blizzard.dto.WowClassDTO;
import com.bebis.BeBiS.profile.domain.WowCharacter;
import com.bebis.BeBiS.profile.dto.CharacterSyncData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class ProfileTestData {
    public static final int CHAR_MAX_NAME_LENGTH = 12;
    public static final int MAX_LEVEL = 60;
    public static List<RaceDTO> races = Arrays.stream(WowCharacter.Race.values())
            .map(race -> new RaceDTO(race.ordinal(), race.name(), null))
            .toList();

    public static List<WowClassDTO> classes = Arrays.stream(WowCharacter.WowClass.values())
            .map(wowClass -> new WowClassDTO(wowClass.ordinal(), wowClass.name(), null))
            .toList();

    public static final int DEFAULT_ACCOUNT_COUNT = 1;
    public static final int DEFAULT_CHAR_COUNT = 5;
    public static final int DEFAULT_CHAR_LEVEL = 60;

    private static final ProfileMapper MAPPER = new ProfileMapper();

    public static List<CharacterSyncData> mapToSyncData(ProfileSummaryResponse response, long accountId) {
        return MAPPER.mapToSyncData(response, accountId);
    }

    public static CharacterSyncData fromDTO(com.bebis.BeBiS.integration.blizzard.dto.WowCharacterDTO dto, long accountId) {
        return MAPPER.fromDTO(dto, accountId);
    }

    public static ProfileSummaryResponse generateProfileSummaryResponse(Integer numberOfAccounts, Integer numberOfCharactersPerAccount) {
        return new ProfileSummaryResponse(generateWowAccountDTOList(numberOfAccounts, numberOfCharactersPerAccount));
    }

    public static ProfileSummaryResponse generateProfileSummaryResponse(List<WowAccountDTO> accounts) {
        return new ProfileSummaryResponse(accounts);
    }

    public static ProfileSummaryResponse generateProfileSummaryResponse() {
        return new ProfileSummaryResponse(generateWowAccountDTOList(DEFAULT_ACCOUNT_COUNT, DEFAULT_CHAR_COUNT));
    }

    public static List<WowAccountDTO> generateWowAccountDTOList(Integer numberOfAccounts, Integer numberOfCharactersPerAccount) {
        if (numberOfAccounts == null || numberOfCharactersPerAccount == null) {
            return null;
        }
        List<WowAccountDTO> wowAccountDTOList = new ArrayList<>();
        for (int id = 0; id < numberOfAccounts; id++) {
            wowAccountDTOList.add(new WowAccountDTO(id, generateWowCharacterDTOList(numberOfCharactersPerAccount)));
        }
        return wowAccountDTOList;
    }

    public static List<WowCharacterDTO> generateWowCharacterDTOList(int numberOfCharacters) {
        List<WowCharacterDTO> wowCharacterDTOList = new ArrayList<>();
        for (int id = 0; id < numberOfCharacters; id++) {
            wowCharacterDTOList.add(generateWowCharacterDTO(id));
        }
        return wowCharacterDTOList;
    }

    public static WowCharacterDTO generateWowCharacterDTO(long id, String name, String realmName) {
        return new WowCharacterDTO(
                id,
                name,
                DEFAULT_CHAR_LEVEL,
                null,
                new RealmDTO(1, realmName, realmName.toLowerCase(), null),
                getRandomRaceDTO(),
                getRandomWowClassDTO()
        );
    }

    public static WowCharacterDTO generateWowCharacterDTO(int id) {
        Random rand = new Random();
        int nameLength = rand.nextInt(CHAR_MAX_NAME_LENGTH);
        int level = rand.nextInt(MAX_LEVEL);
        return new WowCharacterDTO(
                id,
                generateRandomName(nameLength),
                level,
                null,
                new RealmDTO(1, "Soulseeker", "soulseeker", null), // overload to provide a custom realm if needed
                getRandomRaceDTO(),
                getRandomWowClassDTO()
        );
    }

    public static RaceDTO getRandomRaceDTO() {
        // not real id + name pairs, will have to look it up and change it in the future
        return races.get((int) (Math.random() * races.size()));
    }

    public static WowClassDTO getRandomWowClassDTO() {
        return classes.get((int) (Math.random() * classes.size()));
    }

    public static SpecializationResponse generateSpecializationResponse(List<String> treeNames, List<Integer> pointsSpentDistribution) {
        List<SpecializationResponse.CharacterSpecialization> specs = new ArrayList<>();

        for (int i = 0; i < treeNames.size(); i++) {
            String name = treeNames.get(i);
            Integer points = (i < pointsSpentDistribution.size()) ? pointsSpentDistribution.get(i) : 0;

            SpecializationResponse.SpecializationReference specRef =
                    new SpecializationResponse.SpecializationReference(null, name, (long) i);

            specs.add(new SpecializationResponse.CharacterSpecialization(specRef, List.of(), points));
        }

        SpecializationResponse.CharacterReference charRef =
                new SpecializationResponse.CharacterReference(null, "Thelamar", 1L, null);

        return new SpecializationResponse(null, specs, charRef);
    }

    public static SpecializationResponse generateSpecializationResponseWithNullSpecs() {
        SpecializationResponse.CharacterReference charRef =
                new SpecializationResponse.CharacterReference(null, "Thelamar", 1L, null);

        return new SpecializationResponse(null, null, charRef);
    }

    private static String generateRandomName(int length) {
        String charPool = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        StringBuilder randomString = new StringBuilder();
        for (int i = 0; i < length + 3; i++) {
            int index = (int) (Math.random() * charPool.length());
            randomString.append(charPool.charAt(index));
        }
        return randomString.toString();
    }

}
