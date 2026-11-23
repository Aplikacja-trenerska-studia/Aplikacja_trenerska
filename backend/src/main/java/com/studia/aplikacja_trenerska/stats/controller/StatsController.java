package com.studia.aplikacja_trenerska.stats.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.studia.aplikacja_trenerska.stats.dto.StatsDtos.SessionResponse;
import com.studia.aplikacja_trenerska.stats.dto.StatsDtos.StatsResponse;
import com.studia.aplikacja_trenerska.stats.service.StatsService;

@RestController
@RequestMapping("/api/stats")
@PreAuthorize("hasAnyRole('USER', 'TRAINER', 'ADMIN')")
public class StatsController {
    private final StatsService stats;

    public StatsController(StatsService stats) {
        this.stats = stats;
    }

    @GetMapping("/summary")
    public StatsResponse summary(@AuthenticationPrincipal UserDetails principal,
            @RequestParam(defaultValue = "12") int weeks, @RequestParam(defaultValue = "Europe/Warsaw") String zone) {
        return stats.summary(principal.getUsername(), weeks, zone);
    }

    @GetMapping("/sessions")
    public List<SessionResponse> sessions(@AuthenticationPrincipal UserDetails principal,
            @RequestParam(defaultValue = "5") int limit) {
        return stats.recentSessions(principal.getUsername(), limit);
    }
}
