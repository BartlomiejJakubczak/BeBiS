package com.bebis.BeBiS.profile;

import com.bebis.BeBiS.integration.blizzard.dto.SpecializationResponse;
import com.bebis.BeBiS.profile.domain.WowTalents;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

import static com.bebis.BeBiS.profile.domain.WowTalents.Specialization;


@Component
class SpecializationMapper {

    public Optional<WowTalents> fromDTO(SpecializationResponse response) {
        if (response == null || response.specializations() == null) {
            return Optional.empty();
        }
        List<Specialization> talents = response.specializations()
                .stream()
                .map(spec -> new Specialization(
                        spec.specialization().name().toUpperCase(),
                        spec.pointsSpent() != null ? spec.pointsSpent() : 0
                )) // map doesn't act on empty lists, so it's safe
                .toList();
        return Optional.of(new WowTalents(talents));
    }

}
