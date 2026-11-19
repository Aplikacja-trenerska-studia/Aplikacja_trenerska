package com.studia.aplikacja_trenerska.trainer.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import com.studia.aplikacja_trenerska.trainer.dto.TrainerDtos.*;
import com.studia.aplikacja_trenerska.trainer.service.TrainerService;

@RestController
@RequestMapping("/api")
@PreAuthorize("hasAnyRole('USER', 'TRAINER', 'ADMIN')")
public class TraineeController {
    private final TrainerService trainer;
    public TraineeController(TrainerService trainer) { this.trainer = trainer; }

    @GetMapping("/my-invitations") public List<InvitationResponse> invitations(@AuthenticationPrincipal UserDetails user) {
        return trainer.invitations(user.getUsername());
    }
    @PostMapping("/my-invitations/{groupId}/accept") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void accept(@AuthenticationPrincipal UserDetails user, @PathVariable Long groupId) {
        trainer.accept(user.getUsername(), groupId);
    }
    @DeleteMapping("/my-invitations/{groupId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(@AuthenticationPrincipal UserDetails user, @PathVariable Long groupId) {
        trainer.leave(user.getUsername(), groupId);
    }
    @GetMapping("/my-assignments") public List<AssignmentResponse> assignments(@AuthenticationPrincipal UserDetails user) {
        return trainer.assignments(user.getUsername());
    }
}
