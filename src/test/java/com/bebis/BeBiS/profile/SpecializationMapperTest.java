package com.bebis.BeBiS.profile;

import com.bebis.BeBiS.integration.blizzard.BlizzardFixtures;
import com.bebis.BeBiS.integration.blizzard.dto.SpecializationResponse;
import com.bebis.BeBiS.profile.domain.ClassSpec;
import com.bebis.BeBiS.profile.domain.WowCharacter.WowClass;
import com.bebis.BeBiS.profile.domain.WowTalents;
import com.bebis.BeBiS.profile.domain.exception.InvalidTalentTreeException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static com.bebis.BeBiS.profile.domain.ClassSpec.getClassSpec;
import static com.bebis.BeBiS.profile.domain.WowTalentsBuilder.newWowTalents;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SpecializationMapperTest {

    private final SpecializationMapper mapper = new SpecializationMapper();

    @Test
    void shouldReturnEmptySpecSetWhenResponseIsNull() {
        // when
        WowTalents result = mapper.fromDTO(null);

        // then
        assertThat(result.specs()).isEmpty();
    }

    @Test
    void shouldReturnEmptySpecSetWhenSpecializationsListIsNull() {
        // given
        SpecializationResponse response = new SpecializationResponse(null);

        // when
        WowTalents result = mapper.fromDTO(response);

        // then
        assertThat(result.specs()).isEmpty();
    }

    @Test
    void shouldThrowWhenSpecNameInResponseIsNull() {
        // given
        List<SpecializationResponse.Specialization> specs = List.of(new SpecializationResponse.Specialization(null, 15));
        SpecializationResponse.SpecializationGroup group = new SpecializationResponse.SpecializationGroup(true, specs);
        SpecializationResponse response = new SpecializationResponse(List.of(group));

        // when / then
        assertThrows(InvalidTalentTreeException.class, () -> mapper.fromDTO(response));
    }

    @Test
    void shouldThrowWhenSpecNameInResponseIsEmpty() {
        // given
        List<SpecializationResponse.Specialization> specs = List.of(new SpecializationResponse.Specialization("", 15));
        SpecializationResponse.SpecializationGroup group = new SpecializationResponse.SpecializationGroup(true, specs);
        SpecializationResponse response = new SpecializationResponse(List.of(group));

        // when / then
        assertThrows(InvalidTalentTreeException.class, () -> mapper.fromDTO(response));
    }

    /**
     * Contract tests against responses recorded from the real Blizzard API (src/test/resources/blizzard/specs).
     * Each recording must map to the talents it shows and resolve to the spec a player would name.
     */
    @Nested
    class RecordedBlizzardResponses {

        @ParameterizedTest
        @MethodSource("recordedSpecializations")
        void shouldMapRecordedResponse_ToActiveGroupTalentsAndClassSpec(String fixture, WowTalents expectedTalents,
                                                                        WowClass wowClass, ClassSpec expectedSpec) {
            // given
            SpecializationResponse response = BlizzardFixtures.loadSpecialization(fixture);

            // when
            WowTalents result = mapper.fromDTO(response);

            // then
            assertThat(result).isEqualTo(expectedTalents);
            assertThat(getClassSpec(wowClass, result)).isEqualTo(expectedSpec);
        }

        static Stream<Arguments> recordedSpecializations() {
            return Stream.of(
                    // Subtlety has 0 points, so Blizzard leaves it out
                    Arguments.of("spent_points_above_10.json",
                            newWowTalents().withSpec("COMBAT", 32).withSpec("ASSASSINATION", 12).build(),
                            WowClass.ROGUE, ClassSpec.ROGUE_COMBAT),
                    // dual spec, active group first; "Feral Combat" has a space
                    Arguments.of("druid_with_feral.json",
                            newWowTalents().withSpec("FERALCOMBAT", 33).withSpec("BALANCE", 11).withSpec("RESTORATION", 7).build(),
                            WowClass.DRUID, ClassSpec.DRUID_FERAL),
                    // dual spec, inactive Marksmanship 34 group first: reading the first group would give Marksmanship
                    Arguments.of("hunter_with_beast_mastery.json",
                            newWowTalents().withSpec("BEASTMASTERY", 34).withSpec("SURVIVAL", 17).build(),
                            WowClass.HUNTER, ClassSpec.HUNTER_BEASTMASTERY),
                    // no points spent: the active group has no "specializations" key at all
                    Arguments.of("unspent_points_above_10.json",
                            newWowTalents().build(),
                            WowClass.ROGUE, ClassSpec.ROGUE_NONE)
            );
        }
    }
}
