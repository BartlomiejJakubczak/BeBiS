package com.bebis.BeBiS.engine.upgrade;

import com.bebis.BeBiS.equipment.domain.Equipment;
import com.bebis.BeBiS.profile.domain.ClassSpec;
import com.bebis.BeBiS.profile.domain.WowCharacter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Strict representation of /benchmark fixture.
 *
 * @param spec  class + spec of the character
 * @param race  race of the character
 * @param level level of the character
 * @param slots map representing slot to id references of bis items and their alternatives
 */
@JsonIgnoreProperties({"source", "extraCompetitorsSource"})
public record BenchmarkReference(ClassSpec spec, WowCharacter.Race race, int level,
                                 Map<Equipment.Slot, SlotReference> slots) {

    private static final EnumSet<Equipment.Slot> REQUIRED_SLOTS = EnumSet.of(Equipment.Slot.HEAD, Equipment.Slot.NECK,
            Equipment.Slot.SHOULDER, Equipment.Slot.BACK, Equipment.Slot.CHEST, Equipment.Slot.WRIST, Equipment.Slot.HANDS,
            Equipment.Slot.WAIST, Equipment.Slot.LEGS, Equipment.Slot.FEET, Equipment.Slot.RANGED);

    public BenchmarkReference {
        if (level < 1 || level > 60)
            throw new IllegalArgumentException("level must be between 1 and 60");

        slots = Collections.unmodifiableMap(new EnumMap<>(slots));

        if (!areAllSlotsPresent(slots.keySet()))
            throw new IllegalArgumentException("Not all slots are present in json");

        if (!areRivalsPresent(slots))
            throw new IllegalArgumentException("Not all slots have rivals");
    }

    private boolean areRivalsPresent(Map<Equipment.Slot, SlotReference> slots) {
        return slots.values().stream().allMatch((slot) -> slot.candidates().size() > 1);
    }

    private boolean areAllSlotsPresent(Set<Equipment.Slot> slots) {
        return slots.containsAll(REQUIRED_SLOTS);
    }

    public long bestItemId(Equipment.Slot slot) {
        return slots.get(slot).best().getFirst();
    }

    public record SlotReference(List<Long> best, List<Long> alternatives, List<Long> extraCompetitors) {

        public SlotReference {
            if (best.size() != 1) throw new IllegalArgumentException("Best slot has to be singular");
            best = List.copyOf(best);

            alternatives = List.copyOf(alternatives);
            extraCompetitors = List.copyOf(extraCompetitors);
        }

        public Set<Long> candidates() {
            return Stream.of(best(), alternatives(), extraCompetitors()).flatMap(Collection::stream).collect(Collectors.toSet());
        }
    }
}
