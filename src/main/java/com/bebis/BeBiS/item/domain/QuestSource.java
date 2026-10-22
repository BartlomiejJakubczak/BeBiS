package com.bebis.BeBiS.item.domain;

import com.bebis.BeBiS.profile.domain.WowCharacter;

import java.util.Set;

/**
 * The item is a reward of a non-repeatable quest, so a character can obtain it only once. Exported from VMaNGOS (see
 * {@code tools/vmangos-export}), one per quest: items rewarded by mirrored Alliance/Horde quests have one source each.
 * <p>
 * Class and race restrictions here are <b>acquisition</b> constraints of the quest, not equip constraints of the item
 * ({@link Item.ItemMetadata#playableClasses()}). An empty set means the quest has no such restriction.
 *
 * @param questId        VMaNGOS / Blizzard quest id
 * @param questLevel     the level the quest is designed for
 * @param minLevel       the lowest level that can pick the quest up, the real level gate for rewards with no
 *                       required level of their own
 * @param rewardKind     whether the item is the quest's guaranteed reward or one of its choices
 * @param allowedClasses classes that can take the quest, empty if every class can
 * @param allowedRaces   races that can take the quest, empty if every race can
 */
public record QuestSource(
        long questId,
        int questLevel,
        int minLevel,
        RewardKind rewardKind,
        Set<WowCharacter.WowClass> allowedClasses,
        Set<WowCharacter.Race> allowedRaces
) implements Item.Source {

    public QuestSource {
        allowedClasses = Set.copyOf(allowedClasses);
        allowedRaces = Set.copyOf(allowedRaces);
    }

    @Override
    public boolean allows(WowCharacter.Race race) {
        return allowedRaces.isEmpty() || allowedRaces.contains(race);
    }

    @Override
    public boolean allows(WowCharacter.WowClass wowClass) {
        return allowedClasses.isEmpty() || allowedClasses.contains(wowClass);
    }
}
