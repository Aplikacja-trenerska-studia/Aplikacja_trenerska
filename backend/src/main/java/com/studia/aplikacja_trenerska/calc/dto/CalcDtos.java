package com.studia.aplikacja_trenerska.calc.dto;

import java.math.BigDecimal;
import java.util.List;

public final class CalcDtos {
    private CalcDtos() { }

    public enum Sex { MALE, FEMALE }

    public enum ActivityLevel {
        SEDENTARY(1.2), LIGHT(1.375), MODERATE(1.55), ACTIVE(1.725), VERY_ACTIVE(1.9);

        private final double factor;

        ActivityLevel(double factor) { this.factor = factor; }

        public double factor() { return factor; }
    }

    public enum Goal { LOSE, MAINTAIN, GAIN }

    public record OneRepMaxResponse(BigDecimal weight, int reps, BigDecimal epley, BigDecimal brzycki,
            BigDecimal lombardi, List<Percentage> percentages) { }

    public record Percentage(int percent, BigDecimal weight, int reps) { }

    public record CalorieResponse(int bmr, int tdee, int targetCalories, int calorieChange, double activityFactor,
            Macros macros) { }

    public record Macros(int proteinG, int fatG, int carbsG, int proteinKcal, int fatKcal, int carbsKcal) { }
}
