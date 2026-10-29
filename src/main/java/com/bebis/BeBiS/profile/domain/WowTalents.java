package com.bebis.BeBiS.profile.domain;

import java.util.Comparator;
import java.util.Optional;
import java.util.Set;

/**
 * The points a character has spent per talent tree, taken from the active talent group (Classic Era has dual spec).
 * <p>
 * Only trees with points are listed, because Blizzard leaves 0-point trees out. An empty set means no talents: the
 * character is below level 10, hasn't spent any points, or the talents couldn't be fetched. Tree names are the
 * normalized ones the mapper produces ({@code "BEASTMASTERY"}, {@code "FERALCOMBAT"}), which {@link ClassSpec} matches.
 */
public record WowTalents(Set<Specialization> specs) {

    private static final String NO_SPEC = "NONE";

    public WowTalents {
        specs = Set.copyOf(specs);
    }

    /**
     * One talent tree and the points spent in it.
     */
    public record Specialization(String name, int points) {
    }

    /**
     * The tree with the most points, which decides the character's {@link ClassSpec}.
     *
     * @return the tree's name, or {@code "NONE"} when no points are spent or the top trees tie
     */
    public String getActiveSpec() {
        Optional<Specialization> topSpec = specs.stream().max(Comparator.comparingInt(Specialization::points));

        return topSpec.map((top) -> {
            long topSpecCount = specs.stream().filter((s) -> s.points == top.points).count();
            if (topSpecCount > 1) return NO_SPEC; // for now just NO_SPEC to remain deterministic
            return top.name;
        }).orElse(NO_SPEC);
    }

    public static WowTalents none() {
        return new WowTalents(Set.of());
    }
}
