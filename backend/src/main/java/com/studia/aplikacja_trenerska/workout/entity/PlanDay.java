package com.studia.aplikacja_trenerska.workout.entity;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "plan_days", uniqueConstraints = @UniqueConstraint(
        name = "uq_plan_days_position", columnNames = {"plan_id", "day_index"}))
public class PlanDay {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private WorkoutPlan plan;

    @Column(name = "day_index", nullable = false)
    private int dayIndex;

    @Column(nullable = false, length = 120)
    private String name;

    @OneToMany(mappedBy = "planDay", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<PlanExercise> exercises = new ArrayList<>();

    public Long getId() { return id; }
    public WorkoutPlan getPlan() { return plan; }
    void setPlan(WorkoutPlan plan) { this.plan = plan; }
    public int getDayIndex() { return dayIndex; }
    public void setDayIndex(int dayIndex) { this.dayIndex = dayIndex; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<PlanExercise> getExercises() { return java.util.Collections.unmodifiableList(exercises); }

    public void addExercise(PlanExercise exercise) {
        if (exercise.getPlanDay() != null && exercise.getPlanDay() != this) {
            throw new IllegalArgumentException("Exercise entry already belongs to another day");
        }
        if (!exercises.contains(exercise)) {
            exercises.add(exercise);
            exercise.setPlanDay(this);
        }
    }

    public void removeExercise(PlanExercise exercise) {
        if (exercises.remove(exercise)) exercise.setPlanDay(null);
    }
}
