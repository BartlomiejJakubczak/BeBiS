package com.bebis.BeBiS.profile;

import com.bebis.BeBiS.profile.dto.CharacterSyncData;
import com.bebis.BeBiS.profile.jpa.WowCharacterEntity;
import com.bebis.BeBiS.profile.jpa.WowCharacterEntityFactory;
import com.bebis.BeBiS.profile.jpa.WowCharacterRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ProfileSynchronizer {

    private final WowCharacterRepository characterRepository;
    private final WowCharacterEntityFactory characterEntityFactory;

    public ProfileSynchronizer(WowCharacterRepository characterRepository, WowCharacterEntityFactory characterEntityFactory) {
        this.characterRepository = characterRepository;
        this.characterEntityFactory = characterEntityFactory;
    }

    public List<WowCharacterEntity> synchronize(List<CharacterSyncData> fromSummary, long blizzardAccountId) {
        // these come from repo, so they are in MANAGED state
        Map<Long, WowCharacterEntity> existingMap = convertToMap(characterRepository.findAllByPk_BlizzardAccountId(blizzardAccountId));
        List<WowCharacterEntity> toSave = new ArrayList<>();
        Set<Long> processedIds = new HashSet<>();
        boolean modifiedExisting = false;
        for (CharacterSyncData fresh : fromSummary) {
            long id = fresh.characterId();
            processedIds.add(id);
            if (existingMap.containsKey(id)) {
                // update existing entity
                WowCharacterEntity existing = existingMap.get(id);
                // will get updated, because this entity came from repo so it is managed
                modifiedExisting |= existing.updateFrom(fresh);
            } else {
                // create a new one
                WowCharacterEntity newEntity = characterEntityFactory.createNewCharacter(fresh);
                toSave.add(newEntity);
            }
        }
        // clean up deleted or moved characters
        List<WowCharacterEntity> toDelete = existingMap.values().stream()
                .filter(e -> !processedIds.contains(e.getPk().getId()))
                .toList();

        if (!toDelete.isEmpty()) {
            characterRepository.deleteAll(toDelete);
        }

        if (!toSave.isEmpty()) {
            characterRepository.saveAll(toSave);
        }

        // REFRESH: Fetch everything one last time to ensure the "toSave" list
        // actually contains every character (both updated and new).
        if (toDelete.isEmpty() && toSave.isEmpty() && !modifiedExisting) {
            return new ArrayList<>(existingMap.values()); // if repo is in sync with the summary no need to get the repo data the 2nd time
        } else {
            return characterRepository.findAllByPk_BlizzardAccountId(blizzardAccountId); // re-sync just to be sure
        }
    }

    private Map<Long, WowCharacterEntity> convertToMap(List<WowCharacterEntity> entities) {
        return entities.stream().collect(
                Collectors.toMap(
                        (entity) -> entity.getPk().getId(),
                        (entity) -> entity
                )
        );
    }
}
