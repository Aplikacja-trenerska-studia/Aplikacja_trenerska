package com.studia.aplikacja_trenerska.workout.mapper;

import java.util.Comparator;

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
        return new WorkoutPlanSummaryResponse(plan.getId(), plan.getName(), plan.getDescription(), plan.getCreatedAt());
    }

    public WorkoutPlanResponse toResponse(WorkoutPlan plan) {
        var days = plan.getDays().stream().sorted(Comparator.comparingInt(PlanDay::getDayIndex))
                .map(day -> new WorkoutPlanResponse.Day(day.getId(), day.getName(), day.getDayIndex(),
                        day.getExercises().stream().sorted(Comparator.comparingInt(PlanExercise::getOrderIndex))
                                .map(entry -> new WorkoutPlanResponse.Entry(entry.getId(), entry.getOrderIndex(),
                                        exercises.toResponse(entry.getExercise()), entry.getSets(), entry.getReps(), entry.getWeight()))
                                .toList()))
                .toList();
        return new WorkoutPlanResponse(plan.getId(), plan.getName(), plan.getDescription(), plan.getCreatedAt(), days);
    }
}
