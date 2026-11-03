package com.bebis.BeBiS.item.domain;

import com.bebis.BeBiS.item.EquippableItemBuilder;
import com.bebis.BeBiS.profile.domain.WowCharacter.Race;
import com.bebis.BeBiS.profile.domain.WowCharacter.WowClass;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ItemTest {

    private static final Set<Race> ALLIANCE = Set.of(Race.HUMAN, Race.DWARF, Race.NIGHT_ELF, Race.GNOME);
    private static final Set<Race> HORDE = Set.of(Race.ORC, Race.UNDEAD, Race.TAUREN, Race.TROLL);

    @ParameterizedTest
    @MethodSource("raceObtainability")
    void shouldBeObtainableByRace_WhenAnySourceAllowsIt(Set<Item.Source> sources, Race race, boolean expected) {
        // given
        Item item = EquippableItemBuilder.newRing().withSources(sources.toArray(Item.Source[]::new)).build();

        // when / then
        assertThat(item.isObtainableBy(race)).isEqualTo(expected);
    }

    static Stream<Arguments> raceObtainability() {
        return Stream.of(
                Arguments.of(Set.of(), Race.ORC, true), // sources unknown
                Arguments.of(Set.of(quest(1, 60, Set.of(), Set.of())), Race.ORC, true), // quest without race restriction
                Arguments.of(Set.of(quest(1, 60, Set.of(), ALLIANCE)), Race.ORC, false),
                Arguments.of(Set.of(quest(1, 60, Set.of(), ALLIANCE), quest(2, 60, Set.of(), HORDE)), Race.ORC, true), // mirrors
                Arguments.of(Set.of(quest(1, 60, Set.of(), ALLIANCE), new OtherSource()), Race.ORC, true) // e.g. also a drop
        );
    }

    @ParameterizedTest
    @MethodSource("classObtainability")
    void shouldBeObtainableByClass_WhenAnySourceAllowsIt(Set<Item.Source> sources, WowClass wowClass, boolean expected) {
        // given
        Item item = EquippableItemBuilder.newRing().withSources(sources.toArray(Item.Source[]::new)).build();

        // when / then
        assertThat(item.isObtainableBy(wowClass)).isEqualTo(expected);
    }

    static Stream<Arguments> classObtainability() {
        return Stream.of(
                Arguments.of(Set.of(), WowClass.PALADIN, true),
                Arguments.of(Set.of(quest(1, 60, Set.of(WowClass.WARRIOR), Set.of())), WowClass.PALADIN, false), // class quest
                Arguments.of(Set.of(quest(1, 60, Set.of(WowClass.WARRIOR, WowClass.PALADIN), Set.of())), WowClass.PALADIN, true)
        );
    }

    @ParameterizedTest
    @MethodSource("levelObtainability")
    void shouldBeObtainableByLevel_WhenAnySourceAllowsIt(Set<Item.Source> sources, int level, boolean expected) {
        // given
        Item item = EquippableItemBuilder.newRing()
                .withRequiredLevel(0)
                .withSources(sources.toArray(Item.Source[]::new))
                .build();

        // when / then
        assertThat(item.isObtainableBy(level)).isEqualTo(expected);
    }

    static Stream<Arguments> levelObtainability() {
        return Stream.of(
                Arguments.of(Set.of(quest(1, 30, Set.of(), Set.of())), 29, false),
                Arguments.of(Set.of(quest(1, 30, Set.of(), Set.of())), 30, true),
                Arguments.of(Set.of(quest(1, 35, Set.of(), Set.of())), 30, false),
                Arguments.of(Set.of(quest(1, 35, Set.of(), Set.of())), 50, true),
                Arguments.of(Set.of(quest(1, 35, Set.of(), Set.of()), other()), 30, true), // the paradigm is "any source allows it"
                Arguments.of(Set.of(quest(1, 30, Set.of(), Set.of()), quest(2, 50, Set.of(), Set.of())), 35, true),
                Arguments.of(Set.of(quest(1, 30, Set.of(), Set.of()), quest(2, 50, Set.of(), Set.of())), 29, false)
        );
    }

    @Test
    void shouldBeObtainableByLevel_WhenNoSourceAndNoRequiredLevel() {
        // given (required level 0 on purpose: an earlier rule treated 0 as "maybe a quest reward" and dropped these items)
        Item item = EquippableItemBuilder.newRing()
                .withRequiredLevel(0)
                .build();

        // when / then
        assertThat(item.isObtainableBy(1)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("copyLimits")
    void shouldBeEquippableOnce_WhenUniqueOrObtainableOnlyOnce(boolean unique, Set<Item.Source> sources, boolean expected) {
        // given
        Item item = EquippableItemBuilder.newRing().withUniqueEquipped(unique).withSources(sources.toArray(Item.Source[]::new)).build();

        // when / then
        assertThat(item.isEquippableOnce()).isEqualTo(expected);
    }

    static Stream<Arguments> copyLimits() {
        return Stream.of(
                Arguments.of(false, Set.of(), false), // sources unknown: assume repeatable
                Arguments.of(true, Set.of(), true),
                Arguments.of(true, Set.of(new OtherSource()), true), // Unique wins over a repeatable source
                Arguments.of(false, Set.of(quest(1, 30, Set.of(), Set.of())), true),
                Arguments.of(false, Set.of(quest(1, 30, Set.of(), ALLIANCE), quest(2, 30, Set.of(), HORDE)), true), // mirrors (A1)
                Arguments.of(false, Set.of(new OtherSource()), false),
                Arguments.of(false, Set.of(quest(1, 30, Set.of(), Set.of()), new OtherSource()), true) // conservative (A3)
        );
    }

    private static QuestSource quest(long questId, int minLevel, Set<WowClass> classes, Set<Race> races) {
        return new QuestSource(questId, 40, minLevel, RewardKind.CHOICE, classes, races);
    }

    private static OtherSource other() {
        return new OtherSource();
    }
}
