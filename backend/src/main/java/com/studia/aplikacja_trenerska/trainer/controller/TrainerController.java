package com.studia.aplikacja_trenerska.trainer.controller;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import com.studia.aplikacja_trenerska.progress.dto.BodyMetricResponse;
import com.studia.aplikacja_trenerska.progress.dto.ExerciseResultResponse;
import com.studia.aplikacja_trenerska.trainer.dto.TrainerDtos.*;
import com.studia.aplikacja_trenerska.trainer.service.TrainerService;

@RestController
@RequestMapping("/api/groups")
@PreAuthorize("hasRole('TRAINER')")
public class TrainerController {
    private final TrainerService trainer;
    public TrainerController(TrainerService trainer) { this.trainer = trainer; }

    @GetMapping public List<GroupResponse> list(@AuthenticationPrincipal UserDetails user) {
        return trainer.list(user.getUsername());
    }
    @PostMapping public ResponseEntity<GroupResponse> create(@AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody GroupRequest request) {
        GroupResponse group = trainer.create(user.getUsername(), request);
        return ResponseEntity.created(URI.create("/api/groups/" + group.id())).body(group);
    }
    @GetMapping("/{groupId}") public GroupResponse get(@AuthenticationPrincipal UserDetails user, @PathVariable Long groupId) {
        return trainer.get(user.getUsername(), groupId);
    }
    @DeleteMapping("/{groupId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UserDetails user, @PathVariable Long groupId) {
        trainer.delete(user.getUsername(), groupId);
    }
    @PostMapping("/{groupId}/members") public GroupResponse invite(@AuthenticationPrincipal UserDetails user,
            @PathVariable Long groupId, @Valid @RequestBody InviteRequest request) {
        return trainer.invite(user.getUsername(), groupId, request);
    }
    @DeleteMapping("/{groupId}/members/{userId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@AuthenticationPrincipal UserDetails user, @PathVariable Long groupId, @PathVariable Long userId) {
        trainer.removeMember(user.getUsername(), groupId, userId);
    }
    @PostMapping("/{groupId}/assign-plan") public List<AssignmentResponse> assign(@AuthenticationPrincipal UserDetails user,
            @PathVariable Long groupId, @Valid @RequestBody AssignRequest request) {
        return trainer.assign(user.getUsername(), groupId, request);
    }
    @GetMapping("/{groupId}/members/{userId}/results")
    public List<ExerciseResultResponse> results(@AuthenticationPrincipal UserDetails user, @PathVariable Long groupId,
            @PathVariable Long userId, @RequestParam(required = false) Long exerciseId,
            @RequestParam(required = false) Instant from, @RequestParam(required = false) Instant to) {
        return trainer.results(user.getUsername(), groupId, userId, exerciseId, from, to);
    }
    @GetMapping("/{groupId}/members/{userId}/body-metrics")
    public List<BodyMetricResponse> metrics(@AuthenticationPrincipal UserDetails user, @PathVariable Long groupId,
            @PathVariable Long userId, @RequestParam(required = false) Instant from, @RequestParam(required = false) Instant to) {
        return trainer.metrics(user.getUsername(), groupId, userId, from, to);
    }
}
