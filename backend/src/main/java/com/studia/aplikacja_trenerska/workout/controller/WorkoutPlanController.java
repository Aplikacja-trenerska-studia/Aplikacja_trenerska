package com.studia.aplikacja_trenerska.workout.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.studia.aplikacja_trenerska.workout.dto.WorkoutPlanRequest;
import com.studia.aplikacja_trenerska.workout.dto.WorkoutPlanResponse;
import com.studia.aplikacja_trenerska.workout.dto.WorkoutPlanSummaryResponse;
import com.studia.aplikacja_trenerska.workout.service.WorkoutPlanService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/plans")
@PreAuthorize("hasAnyRole('USER', 'TRAINER', 'ADMIN')")
public class WorkoutPlanController {
    private final WorkoutPlanService plans;

    public WorkoutPlanController(WorkoutPlanService plans) {
        this.plans = plans;
    }

    @GetMapping
    public List<WorkoutPlanSummaryResponse> list(@AuthenticationPrincipal UserDetails principal) {
        return plans.list(principal.getUsername());
    }

    @GetMapping("/{id}")
    public WorkoutPlanResponse get(@AuthenticationPrincipal UserDetails principal, @PathVariable Long id) {
        return plans.get(principal.getUsername(), id);
    }

    @PostMapping
    public ResponseEntity<WorkoutPlanResponse> create(@AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody WorkoutPlanRequest request) {
        var plan = plans.create(principal.getUsername(), request);
        return ResponseEntity.created(URI.create("/api/plans/" + plan.id())).body(plan);
    }

    @PutMapping("/{id}")
    public WorkoutPlanResponse update(@AuthenticationPrincipal UserDetails principal, @PathVariable Long id,
            @Valid @RequestBody WorkoutPlanRequest request) {
        return plans.update(principal.getUsername(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UserDetails principal, @PathVariable Long id) {
        plans.delete(principal.getUsername(), id);
    }
}
