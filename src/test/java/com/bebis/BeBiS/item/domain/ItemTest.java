package com.bebis.BeBiS.item.domain;

import com.bebis.BeBiS.item.EquippableItemBuilder;
import com.bebis.BeBiS.profile.domain.WowCharacter.Race;
import com.bebis.BeBiS.profile.domain.WowCharacter.WowClass;
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
                Arguments.of(Set.of(quest(1, Set.of(), Set.of())), Race.ORC, true), // quest without race restriction
                Arguments.of(Set.of(quest(1, Set.of(), ALLIANCE)), Race.ORC, false),
                Arguments.of(Set.of(quest(1, Set.of(), ALLIANCE), quest(2, Set.of(), HORDE)), Race.ORC, true), // mirrors
                Arguments.of(Set.of(quest(1, Set.of(), ALLIANCE), new OtherSource()), Race.ORC, true) // e.g. also a drop
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
                Arguments.of(Set.of(quest(1, Set.of(WowClass.WARRIOR), Set.of())), WowClass.PALADIN, false), // class quest
                Arguments.of(Set.of(quest(1, Set.of(WowClass.WARRIOR, WowClass.PALADIN), Set.of())), WowClass.PALADIN, true)
        );
    }

    private static QuestSource quest(long questId, Set<WowClass> classes, Set<Race> races) {
        return new QuestSource(questId, 40, 30, RewardKind.CHOICE, classes, races);
    }
}
