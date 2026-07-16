package com.bebis.BeBiS.profile;

import com.bebis.BeBiS.integration.blizzard.dto.SpecializationResponse;
import com.bebis.BeBiS.profile.domain.WowTalents;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static com.bebis.BeBiS.profile.domain.WowTalents.Specialization;
import static org.assertj.core.api.Assertions.assertThat;

public class SpecializationMapperTest {

    private final SpecializationMapper mapper = new SpecializationMapper();

    @Test
    void shouldReturnEmptyListWhenResponseIsNull() {
        // when
        Optional<WowTalents> result = mapper.fromDTO(null);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyListWhenSpecializationsListIsNull() {
        // given
        SpecializationResponse response = new SpecializationResponse(null, null, null);

        // when
        Optional<WowTalents> result = mapper.fromDTO(response);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldMapValidSpecializationResponseToDomain() {
        // given
        SpecializationResponse response = ProfileTestData.generateSpecializationResponse(
                List.of("Arms", "Fury", "Protection"),
                List.of(31, 20, 0)
        );

        // when
        Optional<WowTalents> result = mapper.fromDTO(response);
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
}