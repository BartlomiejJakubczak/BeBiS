package com.bebis.BeBiS.profile;

import com.bebis.BeBiS.integration.blizzard.dto.SpecializationResponse;
import com.bebis.BeBiS.profile.domain.WowTalents;
import com.bebis.BeBiS.profile.domain.exception.InvalidTalentTreeException;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.bebis.BeBiS.profile.domain.WowTalents.Specialization;


/**
 * Maps the Classic Era specializations response to {@link WowTalents}.
 * <p>
 * Only the active group is read: with dual spec Blizzard sends two groups, and the active one isn't always first.
 * Tree names are uppercased without spaces, so "Beast Mastery" and "Feral Combat" match {@code ClassSpec}'s
 * {@code BEASTMASTERY} and {@code FERALCOMBAT}.
 */
@Component
final class SpecializationMapper {

    /**
     * @return the active group's talents, or no talents when the response or its groups are missing
     * @throws InvalidTalentTreeException when a tree has no name
     */
    public WowTalents fromDTO(SpecializationResponse response) throws InvalidTalentTreeException {
        if (response == null || response.specializationGroups() == null) {
            return WowTalents.none();
        }
        Set<Specialization> readSpecs = response.specializationGroups().stream()
                .filter(SpecializationResponse.SpecializationGroup::isActive)
                .map(SpecializationResponse.SpecializationGroup::specializations)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .map((spec) -> new Specialization(mapName(spec.name()), spec.spentPoints()))
                .collect(Collectors.toSet());

        return new WowTalents(readSpecs);
    }

    private String mapName(String name) throws InvalidTalentTreeException {
        if (name == null || name.isEmpty())
            throw new InvalidTalentTreeException("Specialization name cannot be null or empty");
        return name.toUpperCase(Locale.ROOT).replace(" ", "");
    }

}
