package com.studia.aplikacja_trenerska.progress.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "body_metrics")
@IdClass(TimeSeriesId.class)
public class BodyMetric {
    @Id private UUID id;
    @Id @Column(nullable = false) private Instant time;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "weight_kg", precision = 6, scale = 2) private BigDecimal weightKg;
    @Column(name = "chest_cm", precision = 6, scale = 2) private BigDecimal chestCm;
    @Column(name = "waist_cm", precision = 6, scale = 2) private BigDecimal waistCm;
    @Column(name = "hips_cm", precision = 6, scale = 2) private BigDecimal hipsCm;
    @Column(name = "arm_cm", precision = 6, scale = 2) private BigDecimal armCm;
    @Column(name = "thigh_cm", precision = 6, scale = 2) private BigDecimal thighCm;

    protected BodyMetric() { }

    public BodyMetric(Instant time, Long userId, BigDecimal weightKg, BigDecimal chestCm,
            BigDecimal waistCm, BigDecimal hipsCm, BigDecimal armCm, BigDecimal thighCm) {
        this.id = UUID.randomUUID();
        this.time = time;
        this.userId = userId;
        this.weightKg = weightKg;
        this.chestCm = chestCm;
        this.waistCm = waistCm;
        this.hipsCm = hipsCm;
        this.armCm = armCm;
        this.thighCm = thighCm;
    }

    public UUID getId() { return id; }
    public Instant getTime() { return time; }
    public Long getUserId() { return userId; }
    public BigDecimal getWeightKg() { return weightKg; }
    public BigDecimal getChestCm() { return chestCm; }
    public BigDecimal getWaistCm() { return waistCm; }
    public BigDecimal getHipsCm() { return hipsCm; }
    public BigDecimal getArmCm() { return armCm; }
    public BigDecimal getThighCm() { return thighCm; }
}
