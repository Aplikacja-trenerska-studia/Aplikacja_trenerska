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
@Table(name = "exercise_results")
@IdClass(TimeSeriesId.class)
public class ExerciseResult {
    @Id private UUID id;
    @Id @Column(nullable = false) private Instant time;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "exercise_id", nullable = false) private Long exerciseId;
    @Column(name = "exercise_name", nullable = false, length = 160) private String exerciseName;
    @Column(name = "plan_name", nullable = false, length = 120) private String planName;
    @Column(name = "day_name", nullable = false, length = 120) private String dayName;
    @Column(name = "set_no", nullable = false) private int setNo;
    @Column(nullable = false) private int reps;
    @Column(nullable = false, precision = 7, scale = 2) private BigDecimal weight;

    protected ExerciseResult() { }

    public ExerciseResult(Instant time, Long userId, Long exerciseId, String exerciseName,
            String planName, String dayName, int setNo, int reps, BigDecimal weight) {
        this.id = UUID.randomUUID();
        this.time = time;
        this.userId = userId;
        this.exerciseId = exerciseId;
        this.exerciseName = exerciseName;
        this.planName = planName;
        this.dayName = dayName;
        this.setNo = setNo;
        this.reps = reps;
        this.weight = weight;
    }

    public UUID getId() { return id; }
    public Instant getTime() { return time; }
    public Long getUserId() { return userId; }
    public Long getExerciseId() { return exerciseId; }
    public String getExerciseName() { return exerciseName; }
    public String getPlanName() { return planName; }
    public String getDayName() { return dayName; }
    public int getSetNo() { return setNo; }
    public int getReps() { return reps; }
    public BigDecimal getWeight() { return weight; }
}
