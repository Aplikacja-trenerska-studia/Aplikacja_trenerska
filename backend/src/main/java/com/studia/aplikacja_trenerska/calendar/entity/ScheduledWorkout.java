package com.studia.aplikacja_trenerska.calendar.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "scheduled_workouts")
public class ScheduledWorkout {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "plan_id") private Long planId;
    @Column(name = "day_index", nullable = false) private int dayIndex;
    @Column(name = "plan_name", nullable = false, length = 120) private String planName;
    @Column(name = "day_name", nullable = false, length = 120) private String dayName;
    @Column(name = "scheduled_date", nullable = false) private LocalDate scheduledDate;
    @Column(length = 500) private String note;
    @Column(name = "completed_at") private Instant completedAt;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS);

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }
    public int getDayIndex() { return dayIndex; }
    public void setDayIndex(int dayIndex) { this.dayIndex = dayIndex; }
    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }
    public String getDayName() { return dayName; }
    public void setDayName(String dayName) { this.dayName = dayName; }
    public LocalDate getScheduledDate() { return scheduledDate; }
    public void setScheduledDate(LocalDate scheduledDate) { this.scheduledDate = scheduledDate; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public Instant getCreatedAt() { return createdAt; }
}
