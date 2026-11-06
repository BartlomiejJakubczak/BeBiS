package com.bebis.BeBiS.engine.power.core;

public final class PowerCalculatorEngineProvider {

    private PowerCalculatorEngineProvider() {
    }

    public static PowerCalculatorEngine engine() {
        AttributeScoreCalculator attrCalculator = new AttributeScoreCalculator(new StatWeightRegistry());
        return new PowerCalculatorEngine(attrCalculator, new WeaponScoreCalculator(new WeaponStatWeightRegistry(), attrCalculator));
    }
}
