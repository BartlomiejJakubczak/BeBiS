package com.bebis.BeBiS.profile.domain;

import com.bebis.BeBiS.profile.domain.exception.InvalidTalentTreeException;
import com.bebis.BeBiS.profile.domain.exception.TalentTreeTieInException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.bebis.BeBiS.profile.domain.WowTalents.Specialization;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class WowTalentsTest {

    @Test
    void shouldThrowExceptionWhenSpecsAreEmpty() {
        // when / then
        assertThrows(InvalidTalentTreeException.class, () -> new WowTalents(List.of()));
    }

    @Test
    void shouldThrowExceptionWhenSpecsAreLessOrGreaterThanThree() {
        // when / then
        assertThrows(InvalidTalentTreeException.class, () -> new WowTalents(
                List.of(new Specialization("FURY", 20), new Specialization("PROTECTION", 15))
        ));
        assertThrows(InvalidTalentTreeException.class, () -> new WowTalents(
                List.of(new Specialization("FURY", 10),
                        new Specialization("PROTECTION", 10),
                        new Specialization("ARMS", 1),
                        new Specialization("FIGHTER", 2))
        ));
    }

    @Test
    void shouldThrowExceptionWhenTalentsContainPointsTieIn() {

        WowTalents talents = new WowTalents(List.of(
                new Specialization("FURY", 15),
                new Specialization("PROTECTION", 15),
                new Specialization("ARMS", 0)
        ));

        assertThrows(TalentTreeTieInException.class, talents::getActiveSpec);

        talents = new WowTalents(List.of(
                new Specialization("FURY", 6),
                new Specialization("PROTECTION", 6),
                new Specialization("ARMS", 6)
        ));

        assertThrows(TalentTreeTieInException.class, talents::getActiveSpec);
    }

    @Test
    void shouldCorrectlyDetermineActiveSpec() {
        // given
        WowTalents talents = new WowTalents(List.of(
                new Specialization("FURY", 15),
                new Specialization("PROTECTION", 10),
                new Specialization("ARMS", 0)
        ));

        // when / then
        assertThat(talents.getActiveSpec()).isEqualTo("FURY");
    }
}
