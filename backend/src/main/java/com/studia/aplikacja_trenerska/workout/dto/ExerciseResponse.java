package com.studia.aplikacja_trenerska.workout.dto;

import com.studia.aplikacja_trenerska.workout.entity.ExerciseType;
import com.studia.aplikacja_trenerska.workout.entity.MuscleGroup;

public record ExerciseResponse(Long id, String name, MuscleGroup muscleGroup, ExerciseType type, String description) {
}
