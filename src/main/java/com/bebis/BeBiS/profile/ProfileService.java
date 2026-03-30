package com.bebis.BeBiS.profile;

import com.bebis.BeBiS.integration.blizzard.BlizzardUserClient;
import com.bebis.BeBiS.profile.jpa.WowCharacterEntity;
import com.bebis.BeBiS.profile.jpa.WowCharacterRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProfileService {

    private final BlizzardUserClient blizzardClient;
    private final WowCharacterRepository characterRepository;
    private final ProfileMapper profileMapper;

    public ProfileService(BlizzardUserClient blizzardClient, WowCharacterRepository characterRepository, ProfileMapper profileMapper) {
        this.blizzardClient = blizzardClient;
        this.characterRepository = characterRepository;
        this.profileMapper = profileMapper;
    }

    public List<WowCharacter> getProfileSummary(long blizzardAccountId) {
        return profileMapper.mapToDomain(syncRepoWithSummary(blizzardAccountId));
    }

    private List<WowCharacterEntity> syncRepoWithSummary(long blizzardAccountId) {
        List<WowCharacterEntity> fromSummary = profileMapper.mapToEntity(blizzardClient.getProfileSummary(), blizzardAccountId);
        // these come from repo, so they are in MANAGED state
        Map<Long, WowCharacterEntity> existingMap = convertToMap(characterRepository.findAllByPk_BlizzardAccountId(blizzardAccountId));
        // The "Golden Sync" mental model (buckets)
        List<WowCharacterEntity> toSave = new ArrayList<>();
        Set<Long> processedIds = new HashSet<>();
        boolean modifiedExisting = false;
        for (WowCharacterEntity fresh : fromSummary) {
            long id = fresh.getPk().getId();
            processedIds.add(id);
            if (existingMap.containsKey(id)) {
                // update existing entity
                WowCharacterEntity existing = existingMap.get(id);
                // will get updated, because this entity came from repo so it is managed
                modifiedExisting |= existing.updateFrom(fresh);
            } else {
                // create a new one
                toSave.add(fresh);
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
            return fromSummary; // if repo is in sync with the summary no need to get the repo data the 2nd time
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
