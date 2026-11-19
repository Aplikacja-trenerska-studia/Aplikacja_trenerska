package com.studia.aplikacja_trenerska.progress.dto;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public record BodyMetricRequest(
        @NotNull Instant time,
        @DecimalMin("10") @DecimalMax("500") @Digits(integer = 3, fraction = 2) BigDecimal weightKg,
        @DecimalMin("10") @DecimalMax("300") @Digits(integer = 3, fraction = 2) BigDecimal chestCm,
        @DecimalMin("10") @DecimalMax("300") @Digits(integer = 3, fraction = 2) BigDecimal waistCm,
        @DecimalMin("10") @DecimalMax("300") @Digits(integer = 3, fraction = 2) BigDecimal hipsCm,
        @DecimalMin("5") @DecimalMax("150") @Digits(integer = 3, fraction = 2) BigDecimal armCm,
        @DecimalMin("5") @DecimalMax("200") @Digits(integer = 3, fraction = 2) BigDecimal thighCm) {
    @AssertTrue(message = "Podaj co najmniej jeden pomiar")
    public boolean isMeasurementPresent() {
        return weightKg != null || chestCm != null || waistCm != null || hipsCm != null || armCm != null || thighCm != null;
    }
}
