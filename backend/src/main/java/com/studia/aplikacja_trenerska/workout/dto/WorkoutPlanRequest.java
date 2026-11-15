package com.studia.aplikacja_trenerska.workout.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record WorkoutPlanRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 2000) String description,
        @NotEmpty @Size(max = 31) List<@NotNull @Valid PlanDayRequest> days) {
}
