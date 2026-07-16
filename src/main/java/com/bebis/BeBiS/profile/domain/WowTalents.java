package com.bebis.BeBiS.profile.domain;

import com.bebis.BeBiS.profile.domain.exception.InvalidTalentTreeException;
import com.bebis.BeBiS.profile.domain.exception.TalentTreeTieInException;

import java.util.Comparator;
import java.util.List;

public record WowTalents(List<Specialization> specs) {

    public static final String NO_SPEC = "NONE";

    public WowTalents {
        if (specs.size() != 3)
            throw new InvalidTalentTreeException("There should always be 3 specs per talent tree");
    }

    public record Specialization(String name, int points) {
    }

    public String getActiveSpec() throws TalentTreeTieInException {
        int maxPoints = specs.stream()
                .max(Comparator.comparingInt(Specialization::points))
                .get()
                .points;

        long topSpecCount = specs.stream()
                .filter((spec) -> spec.points == maxPoints)
                .count();
        if (topSpecCount > 1)
            throw new TalentTreeTieInException("Tie-in in talents detected");

        return specs.stream()
                .filter((spec) -> spec.points == maxPoints)
                .findFirst()
                .get()
                .name;
    }

}
