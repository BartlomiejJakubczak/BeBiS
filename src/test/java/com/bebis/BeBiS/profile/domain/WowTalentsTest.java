package com.bebis.BeBiS.profile.domain;

import org.junit.jupiter.api.Test;

import static com.bebis.BeBiS.profile.domain.WowTalentsBuilder.newWowTalents;
import static org.assertj.core.api.Assertions.assertThat;

public class WowTalentsTest {

    @Test
    void shouldCorrectlyDetermineActiveSpec() {
        // given
        WowTalents talents = newWowTalents().withSpec("FURY", 15).withSpec("PROTECTION", 10).build();

        // when / then
        assertThat(talents.getActiveSpec()).isEqualTo("FURY");
    }

    @Test
    void shouldCorrectlyDetermineNoSpec_UnspentTalentPoints() {
        // given
        WowTalents talents = newWowTalents().build();

        // when / then
        assertThat(talents.getActiveSpec()).isEqualTo("NONE");
    }

    @Test
    void shouldCorrectlyDetermineNoSpec_TalentPointsTieIn() {
        // given
        WowTalents talents = newWowTalents().withSpec("FURY", 15).withSpec("PROTECTION", 15).build();

        // when / then
        assertThat(talents.getActiveSpec()).isEqualTo("NONE");
    }
}
