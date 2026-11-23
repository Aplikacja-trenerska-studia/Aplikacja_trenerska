package com.studia.aplikacja_trenerska.calendar.controller;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.studia.aplikacja_trenerska.calendar.dto.CalendarDtos.AgendaResponse;
import com.studia.aplikacja_trenerska.calendar.dto.CalendarDtos.ScheduleRequest;
import com.studia.aplikacja_trenerska.calendar.dto.CalendarDtos.ScheduledWorkoutResponse;
import com.studia.aplikacja_trenerska.calendar.service.ScheduleService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/schedule")
@PreAuthorize("hasAnyRole('USER', 'TRAINER', 'ADMIN')")
public class ScheduleController {
    private final ScheduleService schedule;

    public ScheduleController(ScheduleService schedule) {
        this.schedule = schedule;
    }

    @GetMapping
    public List<ScheduledWorkoutResponse> list(@AuthenticationPrincipal UserDetails principal,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return schedule.list(principal.getUsername(), from, to);
    }

    @GetMapping("/agenda")
    public AgendaResponse agenda(@AuthenticationPrincipal UserDetails principal,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return schedule.agenda(principal.getUsername(), date);
    }

    @PostMapping
    public ResponseEntity<ScheduledWorkoutResponse> create(@AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody ScheduleRequest request) {
        var entry = schedule.create(principal.getUsername(), request);
        return ResponseEntity.created(URI.create("/api/schedule/" + entry.id())).body(entry);
    }

    @PutMapping("/{id}")
    public ScheduledWorkoutResponse update(@AuthenticationPrincipal UserDetails principal, @PathVariable Long id,
            @Valid @RequestBody ScheduleRequest request) {
        return schedule.update(principal.getUsername(), id, request);
    }

    @PostMapping("/{id}/complete")
    public ScheduledWorkoutResponse complete(@AuthenticationPrincipal UserDetails principal, @PathVariable Long id) {
        return schedule.setCompleted(principal.getUsername(), id, true);
    }

    @DeleteMapping("/{id}/complete")
    public ScheduledWorkoutResponse reopen(@AuthenticationPrincipal UserDetails principal, @PathVariable Long id) {
        return schedule.setCompleted(principal.getUsername(), id, false);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UserDetails principal, @PathVariable Long id) {
        schedule.delete(principal.getUsername(), id);
    }
}
