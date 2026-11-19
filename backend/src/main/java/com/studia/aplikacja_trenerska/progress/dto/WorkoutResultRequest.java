package com.studia.aplikacja_trenerska.progress.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record WorkoutResultRequest(
        @NotNull Instant time,
        @NotNull @Positive Long planId,
        @NotNull @Positive Long planDayId,
        @NotNull @Size(min = 1, max = 500) List<@NotNull @Valid Set> sets) {
    public record Set(
            @NotNull @Positive Long exerciseId,
            @NotNull @Min(1) @Max(100) Integer setNo,
            @NotNull @Min(1) @Max(1000) Integer reps,
            @NotNull @DecimalMin("0") @DecimalMax("2000") @Digits(integer = 4, fraction = 2) BigDecimal weight) { }
}
