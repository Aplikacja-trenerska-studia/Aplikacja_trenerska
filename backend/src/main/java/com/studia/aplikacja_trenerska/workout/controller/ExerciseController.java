package com.studia.aplikacja_trenerska.workout.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.studia.aplikacja_trenerska.workout.dto.ExerciseResponse;
import com.studia.aplikacja_trenerska.workout.entity.ExerciseType;
import com.studia.aplikacja_trenerska.workout.entity.MuscleGroup;
import com.studia.aplikacja_trenerska.workout.service.ExerciseService;

@RestController
@RequestMapping("/api/exercises")
@PreAuthorize("hasAnyRole('USER', 'TRAINER', 'ADMIN')")
public class ExerciseController {
    private final ExerciseService exercises;

    public ExerciseController(ExerciseService exercises) {
        this.exercises = exercises;
    }

    @GetMapping
    public List<ExerciseResponse> list(
            @RequestParam(required = false) MuscleGroup muscleGroup,
            @RequestParam(required = false) ExerciseType type,
            @RequestParam(required = false) String q) {
        return exercises.findExercises(muscleGroup, type, q);
    }
}
