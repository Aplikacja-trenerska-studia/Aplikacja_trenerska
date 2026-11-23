package com.studia.aplikacja_trenerska.workout.controller;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.studia.aplikacja_trenerska.workout.dto.PlanShareResponse;
import com.studia.aplikacja_trenerska.workout.dto.SharedPlanResponse;
import com.studia.aplikacja_trenerska.workout.dto.WorkoutPlanResponse;
import com.studia.aplikacja_trenerska.workout.service.PlanSharingService;

@RestController
@RequestMapping("/api")
@PreAuthorize("hasAnyRole('USER', 'TRAINER', 'ADMIN')")
public class PlanSharingController {
    private final PlanSharingService sharing;

    public PlanSharingController(PlanSharingService sharing) {
        this.sharing = sharing;
    }

    @PostMapping("/plans/{id}/share")
    public PlanShareResponse share(@AuthenticationPrincipal UserDetails principal, @PathVariable Long id) {
        return sharing.share(principal.getUsername(), id);
    }

    @DeleteMapping("/plans/{id}/share")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unshare(@AuthenticationPrincipal UserDetails principal, @PathVariable Long id) {
        sharing.unshare(principal.getUsername(), id);
    }

    @GetMapping("/shared-plans/{token}")
    public SharedPlanResponse view(@AuthenticationPrincipal UserDetails principal, @PathVariable String token) {
        return sharing.view(principal.getUsername(), token);
    }

    @PostMapping("/shared-plans/{token}/copy")
    public ResponseEntity<WorkoutPlanResponse> copy(@AuthenticationPrincipal UserDetails principal,
            @PathVariable String token) {
        var plan = sharing.copy(principal.getUsername(), token);
        return ResponseEntity.created(URI.create("/api/plans/" + plan.id())).body(plan);
    }
}
