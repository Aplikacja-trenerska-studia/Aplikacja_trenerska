package com.studia.aplikacja_trenerska.workout.mapper;

import org.springframework.stereotype.Component;

import com.studia.aplikacja_trenerska.workout.dto.ExerciseResponse;
import com.studia.aplikacja_trenerska.workout.entity.Exercise;

@Component
public class ExerciseMapper {
    public ExerciseResponse toResponse(Exercise exercise) {
        return new ExerciseResponse(exercise.getId(), exercise.getName(), exercise.getMuscleGroup(),
                exercise.getType(), exercise.getDescription());
    }
}
