package com.bebis.BeBiS.profile;

import com.bebis.BeBiS.base.BaseFullStackTest;
import com.bebis.BeBiS.integration.blizzard.dto.SpecializationResponse;
import com.bebis.BeBiS.profile.domain.WowTalents;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static com.bebis.BeBiS.profile.domain.WowTalents.Specialization;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class SpecializationServiceIT extends BaseFullStackTest {

    @Autowired
    private SpecializationService service;

    @Test
    void shouldGetTalentsForCharacterWhenResponseIsCorrect() {
        // given
        String realmSlug = "soulseeker";
        String charName = "Thelamar";

        SpecializationResponse response = ProfileTestData.generateSpecializationResponse(
                List.of("Arms", "Fury", "Protection"),
                List.of(31, 20, 0)
        );

        when(blizzardUserClient.getCharacterSpecialization(eq(realmSlug), eq(charName))).thenReturn(response);

        // when
        Optional<WowTalents> result = service.getTalentsForCharacter(realmSlug, charName);
        List<Specialization> specs = result.get().specs();

        // then
        assertThat(specs.size()).isEqualTo(3);

        assertThat(specs.get(0).name()).isEqualTo("ARMS");
        assertThat(specs.get(0).points()).isEqualTo(31);

        assertThat(specs.get(1).name()).isEqualTo("FURY");
        assertThat(specs.get(1).points()).isEqualTo(20);

        assertThat(specs.get(2).name()).isEqualTo("PROTECTION");
        assertThat(specs.get(2).points()).isEqualTo(0);
    }

    @Test
    void shouldGetEmptyOptionalWhenResponseIsNull() {
        // given
        String realmSlug = "soulseeker";
        String charName = "Thelamar";

        when(blizzardUserClient.getCharacterSpecialization(eq(realmSlug), eq(charName))).thenReturn(null);

        // when
        Optional<WowTalents> result = service.getTalentsForCharacter(realmSlug, charName);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldGetEmptyOptionalWhenResponseHasEmptySpecs() {
        // given
        String realmSlug = "soulseeker";
        String charName = "Thelamar";

        SpecializationResponse response = ProfileTestData.generateSpecializationResponseWithNullSpecs();

        when(blizzardUserClient.getCharacterSpecialization(eq(realmSlug), eq(charName))).thenReturn(response);

        // when
        Optional<WowTalents> result = service.getTalentsForCharacter(realmSlug, charName);

        // then
        assertThat(result).isEmpty();
    }
}
