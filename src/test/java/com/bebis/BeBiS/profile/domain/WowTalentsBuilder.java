package com.bebis.BeBiS.profile.domain;

import java.util.HashSet;
import java.util.Set;

/**
 * Builds {@link WowTalents} with domain tree names, as the mapper produces them ({@code "BEASTMASTERY"},
 * {@code "FERALCOMBAT"}). Without any {@link #withSpec} call it builds a character with no points spent.
 * Like the API, add only trees with points in them: Blizzard leaves 0-point trees out.
 */
public class WowTalentsBuilder {

    private final Set<WowTalents.Specialization> specs = new HashSet<>();

    public static WowTalentsBuilder newWowTalents() {
        return new WowTalentsBuilder();
    }

    public WowTalentsBuilder withSpec(String treeName, int points) {
        specs.add(new WowTalents.Specialization(treeName, points));
        return this;
    }

    public WowTalents build() {
        return new WowTalents(specs);
    }
}
