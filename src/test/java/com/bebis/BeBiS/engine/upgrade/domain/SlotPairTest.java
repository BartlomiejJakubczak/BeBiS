package com.bebis.BeBiS.engine.upgrade.domain;

import com.bebis.BeBiS.item.EquippableItemBuilder;
import com.bebis.BeBiS.item.domain.Item;
import com.bebis.BeBiS.item.domain.QuestSource;
import com.bebis.BeBiS.item.domain.RewardKind;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class SlotPairTest {

    private static final Item PLAIN_RING = EquippableItemBuilder.newRing().withBaseId(1L).withUniqueEquipped(false).build();
    private static final Item OTHER_PLAIN_RING = EquippableItemBuilder.newRing().withBaseId(2L).withUniqueEquipped(false).build();
    private static final Item UNIQUE_RING = EquippableItemBuilder.newRing().withBaseId(3L).withUniqueEquipped(true).build();
    private static final Item OTHER_UNIQUE_RING = EquippableItemBuilder.newRing().withBaseId(4L).withUniqueEquipped(true).build();
    private static final Item QUEST_REWARD_RING = EquippableItemBuilder.newRing().withBaseId(18403L).withUniqueEquipped(false)
            .withSources(new QuestSource(7491L, 60, 60, RewardKind.CHOICE, Set.of(), Set.of()))
            .build();

    @ParameterizedTest
    @MethodSource("copyLimitCases")
    void shouldDetectSameEquippableOnceItemInBothSlots(Item slot1, Item slot2, boolean expected) {
        // given
        SlotPair pair = pair(slot1, slot2);

        // when / then
        assertThat(pair.areSameOnceEquippables()).isEqualTo(expected);
    }

    static Stream<Arguments> copyLimitCases() {
        return Stream.of(
                Arguments.of(UNIQUE_RING, UNIQUE_RING, true),
                Arguments.of(QUEST_REWARD_RING, QUEST_REWARD_RING, true),
                Arguments.of(PLAIN_RING, PLAIN_RING, false), // repeatable, can be worn twice
                Arguments.of(UNIQUE_RING, OTHER_UNIQUE_RING, false), // two different unique items are fine
                Arguments.of(UNIQUE_RING, QUEST_REWARD_RING, false),
                Arguments.of(UNIQUE_RING, null, false),
                Arguments.of(null, null, false)
        );
    }

    @ParameterizedTest
    @MethodSource("equippedMatchCases")
    void shouldCountSlotsHoldingTheEquippedItem(SlotPair candidate, SlotPair equipped, int expected) {
        // when / then
        assertThat(candidate.countEquippedMatches(equipped)).isEqualTo(expected);
    }

    static Stream<Arguments> equippedMatchCases() {
        return Stream.of(
                Arguments.of(pair(PLAIN_RING, OTHER_PLAIN_RING), pair(PLAIN_RING, OTHER_PLAIN_RING), 2),
                Arguments.of(pair(PLAIN_RING, UNIQUE_RING), pair(PLAIN_RING, OTHER_PLAIN_RING), 1),
                Arguments.of(pair(OTHER_PLAIN_RING, PLAIN_RING), pair(PLAIN_RING, OTHER_PLAIN_RING), 0), // same items, swapped slots
                Arguments.of(pair(PLAIN_RING, null), pair(PLAIN_RING, null), 1) // two empty slots aren't a match
        );
    }

    private static SlotPair pair(Item slot1, Item slot2) {
        return new SlotPair(scored(slot1), scored(slot2));
    }

    private static Optional<ScoredItem> scored(Item item) {
        return Optional.ofNullable(item).map(it -> new ScoredItem(it, 10.0));
    }
}
