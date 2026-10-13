package com.bebis.BeBiS.engine.power.core;

import com.bebis.BeBiS.engine.power.domain.StatWeights;
import com.bebis.BeBiS.item.ArmorBuilder;
import com.bebis.BeBiS.item.domain.Item;
import com.bebis.BeBiS.item.domain.StatType;
import com.bebis.BeBiS.profile.domain.ClassSpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static com.bebis.BeBiS.item.domain.StatType.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AttributeScoreCalculatorTest {

    @Mock
    private StatWeightRegistry registry;

    private AttributeScoreCalculator calculator;

    @BeforeEach
    public void setUp() {
        calculator = new AttributeScoreCalculator(registry);
    }

    @Test
    void shouldCalculateWeightedSumOfItemStats() {
        // Given
        ClassSpec classSpec = ClassSpec.WARRIOR_ARMS;

        Item item = ArmorBuilder.newShield()
                .withStats(Map.of("STRENGTH", 10, "STAMINA", 10))
                .build();

        Map<StatType, Double> testWeights = Map.of(STRENGTH, 2.0, STAMINA, 1.5);
        StatWeights weights = new StatWeights(testWeights);

        when(registry.getWeightsFor(classSpec)).thenReturn(weights);

        // When
        double score = calculator.calculate(item, classSpec);

        // Then
        // (10 * 2.0) + (10 * 1.5) = 20.0 + 15 = 35.0
        assertThat(score).isEqualTo(35.0);
    }

    @Test
    void shouldTreatUnmappedStatsAsZeroWeight() {
        // Given
        ClassSpec classSpec = ClassSpec.WARRIOR_ARMS;

        Item item = ArmorBuilder.newShield()
                .withStats(Map.of("STRENGTH", 10, "SPIRIT", 10))
                .build();

        Map<StatType, Double> testWeights = Map.of(STRENGTH, 2.0, SPIRIT, 0.0);
        StatWeights weights = new StatWeights(testWeights);

        when(registry.getWeightsFor(classSpec)).thenReturn(weights);

        // When
        double score = calculator.calculate(item, classSpec);

        // Then
        // (10 * 2.0) + (10 * 0.0) = 20.0 + 0.0 = 20.0
        assertThat(score).isEqualTo(20.0);
    }
}
