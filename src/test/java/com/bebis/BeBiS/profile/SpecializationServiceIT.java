package com.bebis.BeBiS.profile;

import com.bebis.BeBiS.base.BaseFullStackTest;
import com.bebis.BeBiS.integration.blizzard.BlizzardFixtures;
import com.bebis.BeBiS.integration.blizzard.dto.SpecializationResponse;
import com.bebis.BeBiS.profile.domain.WowTalents;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static com.bebis.BeBiS.profile.domain.WowTalentsBuilder.newWowTalents;
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

        SpecializationResponse response = BlizzardFixtures.loadSpecialization("spent_points_above_10.json");

        when(blizzardUserClient.getCharacterSpecialization(eq(realmSlug), eq(charName))).thenReturn(response);

        // when
        WowTalents result = service.getTalentsForCharacter(realmSlug, charName);

        // then
        assertThat(result).isEqualTo(newWowTalents().withSpec("COMBAT", 32).withSpec("ASSASSINATION", 12).build());
    }

    @Test
    void shouldGetWowTalentsWithEmptySpecsWhenResponseIsNull() {
        // given
        String realmSlug = "soulseeker";
        String charName = "Thelamar";

        when(blizzardUserClient.getCharacterSpecialization(eq(realmSlug), eq(charName))).thenReturn(null);

        // when
        WowTalents result = service.getTalentsForCharacter(realmSlug, charName);

        // then
        assertThat(result.specs()).isEmpty();
    }

    @Test
    void shouldGetWowTalentsWithEmptySpecsWhenResponseHasEmptySpecs() {
        // given
        String realmSlug = "soulseeker";
        String charName = "Thelamar";

        SpecializationResponse response = BlizzardFixtures.loadSpecialization("unspent_points_above_10.json");

        when(blizzardUserClient.getCharacterSpecialization(eq(realmSlug), eq(charName))).thenReturn(response);

        // when
        WowTalents result = service.getTalentsForCharacter(realmSlug, charName);

        // then
        assertThat(result.specs()).isEmpty();
    }

    @Test
    void shouldGetWowTalentsWithEmptySpecsWhenInvalidSpecThrownException() {
        // given
        String realmSlug = "soulseeker";
        String charName = "Thelamar";

        List<SpecializationResponse.Specialization> specs = List.of(new SpecializationResponse.Specialization("", 15));
        SpecializationResponse.SpecializationGroup group = new SpecializationResponse.SpecializationGroup(true, specs);
        SpecializationResponse response = new SpecializationResponse(List.of(group));

        when(blizzardUserClient.getCharacterSpecialization(eq(realmSlug), eq(charName))).thenReturn(response);

        // when / then
        WowTalents result = service.getTalentsForCharacter(realmSlug, charName);

        // then
        assertThat(result.specs()).isEmpty();
    }
}
