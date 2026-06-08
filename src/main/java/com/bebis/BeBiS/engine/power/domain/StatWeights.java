package com.bebis.BeBiS.engine.power.domain;

import com.bebis.BeBiS.item.domain.StatType;

import java.util.Map;

public record StatWeights(
        Map<StatType, Double> statWeights
) {
    public double getWeightFor(StatType stat) {
        return statWeights.getOrDefault(stat, 0.0);
    }
}
