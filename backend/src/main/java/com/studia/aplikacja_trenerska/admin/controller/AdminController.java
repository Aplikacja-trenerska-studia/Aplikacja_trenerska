package com.studia.aplikacja_trenerska.admin.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.studia.aplikacja_trenerska.admin.dto.AdminDtos.*;
import com.studia.aplikacja_trenerska.admin.service.AdminService;
import com.studia.aplikacja_trenerska.user.entity.UserRole;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final AdminService admin;

    public AdminController(AdminService admin) { this.admin = admin; }

    @GetMapping("/overview")
    public OverviewResponse overview(@AuthenticationPrincipal UserDetails principal) {
        return admin.overview(principal.getUsername());
    }

    @GetMapping("/users")
    public UserPage users(@AuthenticationPrincipal UserDetails principal,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) AccountStatus status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return admin.list(principal.getUsername(), q, role, status, page, size);
    }

    @PatchMapping("/users/{id}/role")
    public UserResponse role(@AuthenticationPrincipal UserDetails principal, @PathVariable Long id,
            @Valid @RequestBody RoleRequest request) {
        return admin.changeRole(principal.getUsername(), id, request.role());
    }

    @PatchMapping("/users/{id}/block")
    public UserResponse block(@AuthenticationPrincipal UserDetails principal, @PathVariable Long id,
            @Valid @RequestBody BlockRequest request) {
        return admin.changeBlock(principal.getUsername(), id, request.blocked());
    }
}
