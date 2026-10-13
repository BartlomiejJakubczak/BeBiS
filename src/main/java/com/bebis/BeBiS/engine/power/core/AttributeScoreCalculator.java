package com.bebis.BeBiS.engine.power.core;

import com.bebis.BeBiS.engine.power.domain.StatWeights;
import com.bebis.BeBiS.item.domain.Item;
import com.bebis.BeBiS.profile.domain.ClassSpec;
import org.springframework.stereotype.Component;

@Component
final class AttributeScoreCalculator {

    private final StatWeightRegistry registry;

    AttributeScoreCalculator(StatWeightRegistry registry) {
        this.registry = registry;
    }

    public double calculate(Item item, ClassSpec classSpec) {
        return calculateStats(item, registry.getWeightsFor(classSpec));
    }

    private double calculateStats(Item item, StatWeights weights) {
        return item.getMetadata().stats().entrySet().stream()
                .mapToDouble((entry) -> weights.getWeightFor(entry.getKey()) * entry.getValue().doubleValue()).sum();
    }
}
