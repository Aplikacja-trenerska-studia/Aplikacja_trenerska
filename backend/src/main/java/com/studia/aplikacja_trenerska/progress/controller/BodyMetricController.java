package com.studia.aplikacja_trenerska.progress.controller;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.studia.aplikacja_trenerska.progress.dto.BodyMetricRequest;
import com.studia.aplikacja_trenerska.progress.dto.BodyMetricResponse;
import com.studia.aplikacja_trenerska.progress.service.ProgressService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/body-metrics")
@PreAuthorize("hasAnyRole('USER', 'TRAINER', 'ADMIN')")
public class BodyMetricController {
    private final ProgressService progress;

    public BodyMetricController(ProgressService progress) { this.progress = progress; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BodyMetricResponse save(@AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody BodyMetricRequest request) {
        return progress.saveMetric(principal.getUsername(), request);
    }

    @GetMapping
    public List<BodyMetricResponse> list(@AuthenticationPrincipal UserDetails principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return progress.listMetrics(principal.getUsername(), from, to);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UserDetails principal, @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant time) {
        progress.deleteMetric(principal.getUsername(), id, time);
    }
}
