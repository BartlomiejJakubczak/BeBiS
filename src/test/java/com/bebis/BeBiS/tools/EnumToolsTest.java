package com.bebis.BeBiS.tools;

import com.bebis.BeBiS.profile.domain.WowCharacter.Race;
import com.bebis.BeBiS.profile.domain.WowCharacter.WowClass;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class EnumToolsTest {

    @ParameterizedTest
    @MethodSource("raceMasks")
    void shouldDecodeRaceBitmask_WhereBitIdMinusOneMarksARace(int mask, Set<Race> expected) {
        // given / when
        Set<Race> result = EnumTools.fromBitmask(Race.class, mask);

        // then
        assertThat(result).containsExactlyInAnyOrderElementsOf(expected);
    }

    static Stream<Arguments> raceMasks() {
        return Stream.of(
                Arguments.of(0, Set.of()), // no restriction
                Arguments.of(77, Set.of(Race.HUMAN, Race.DWARF, Race.NIGHT_ELF, Race.GNOME)), // alliance
                Arguments.of(178, Set.of(Race.ORC, Race.UNDEAD, Race.TAUREN, Race.TROLL)), // horde
                Arguments.of(8, Set.of(Race.NIGHT_ELF))
        );
    }

    @Test
    void shouldDecodeClassBitmask_SkippingIdsThatDontExistInClassic() {
        // given
        int mask = 1 | 1 << 5 | 1 << 10; // warrior (1), id 6 (death knight, not in classic), druid (11)

        // when
        Set<WowClass> result = EnumTools.fromBitmask(WowClass.class, mask);

        // then
        assertThat(result).containsExactlyInAnyOrder(WowClass.WARRIOR, WowClass.DRUID);
    }

    @Test
    void shouldFindConstantByGameId() {
        // given / when / then
        assertThat(EnumTools.fromId(WowClass.class, 8)).contains(WowClass.MAGE);
        assertThat(EnumTools.fromId(WowClass.class, 6)).isEmpty();
    }
}
