package com.studia.aplikacja_trenerska.progress.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BodyMetricResponse(UUID id, Instant time, BigDecimal weightKg, BigDecimal chestCm,
        BigDecimal waistCm, BigDecimal hipsCm, BigDecimal armCm, BigDecimal thighCm) { }
