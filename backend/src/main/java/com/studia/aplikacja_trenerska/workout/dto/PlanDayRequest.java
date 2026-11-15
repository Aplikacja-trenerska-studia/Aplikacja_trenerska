package com.studia.aplikacja_trenerska.workout.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PlanDayRequest(
        @NotBlank @Size(max = 120) String name,
        @NotEmpty @Size(max = 100) List<@NotNull @Valid PlanExerciseRequest> exercises) {
}
