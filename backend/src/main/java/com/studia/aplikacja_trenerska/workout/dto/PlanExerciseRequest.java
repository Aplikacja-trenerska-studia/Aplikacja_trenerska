package com.studia.aplikacja_trenerska.workout.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PlanExerciseRequest(
        @NotNull @Positive Long exerciseId,
        @NotNull @Positive Integer sets,
        @NotNull @Positive Integer reps,
        @NotNull @DecimalMin("0") @Digits(integer = 5, fraction = 2) BigDecimal weight) {
}
