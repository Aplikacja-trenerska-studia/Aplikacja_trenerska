package com.studia.aplikacja_trenerska.workout.mapper;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.studia.aplikacja_trenerska.workout.dto.WorkoutPlanResponse;
import com.studia.aplikacja_trenerska.workout.dto.WorkoutPlanSummaryResponse;
import com.studia.aplikacja_trenerska.workout.entity.PlanDay;
import com.studia.aplikacja_trenerska.workout.entity.PlanExercise;
import com.studia.aplikacja_trenerska.workout.entity.WorkoutPlan;

@Component
public class WorkoutPlanMapper {
    private final ExerciseMapper exercises;

    public WorkoutPlanMapper(ExerciseMapper exercises) {
        this.exercises = exercises;
    }

    public WorkoutPlanSummaryResponse toSummary(WorkoutPlan plan) {
        return new WorkoutPlanSummaryResponse(plan.getId(), plan.getName(), plan.getDescription(), plan.getCreatedAt(),
                plan.isShared());
    }

    public WorkoutPlanResponse toResponse(WorkoutPlan plan) {
        return new WorkoutPlanResponse(plan.getId(), plan.getName(), plan.getDescription(), plan.getCreatedAt(),
                plan.isShared(), plan.isShared() ? plan.getShareToken() : null, toDays(plan));
    }

    public List<WorkoutPlanResponse.Day> toDays(WorkoutPlan plan) {
        return plan.getDays().stream().sorted(Comparator.comparingInt(PlanDay::getDayIndex))
                .map(day -> new WorkoutPlanResponse.Day(day.getId(), day.getName(), day.getDayIndex(),
                        day.getExercises().stream().sorted(Comparator.comparingInt(PlanExercise::getOrderIndex))
                                .map(entry -> new WorkoutPlanResponse.Entry(entry.getId(), entry.getOrderIndex(),
                                        exercises.toResponse(entry.getExercise()), entry.getSets(), entry.getReps(), entry.getWeight()))
                                .toList()))
                .toList();
    }
}
