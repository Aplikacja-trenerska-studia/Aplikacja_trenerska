package com.studia.aplikacja_trenerska.user.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import com.studia.aplikacja_trenerska.auth.dto.DeleteAccountRequest;
import com.studia.aplikacja_trenerska.auth.dto.MessageResponse;
import com.studia.aplikacja_trenerska.auth.service.AccountService;

import com.studia.aplikacja_trenerska.user.dto.UserResponse;
import com.studia.aplikacja_trenerska.user.mapper.UserMapper;
import com.studia.aplikacja_trenerska.user.repository.UserRepository;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasAnyRole('USER', 'TRAINER', 'ADMIN')")
public class MeController {

	private final UserRepository userRepository;
	private final UserMapper userMapper;
	private final AccountService accountService;

	public MeController(UserRepository userRepository, UserMapper userMapper, AccountService accountService) {
		this.userRepository = userRepository;
		this.userMapper = userMapper;
		this.accountService = accountService;
	}

	@GetMapping("/me")
	public UserResponse me(@AuthenticationPrincipal UserDetails principal) {
		var user = userRepository.findByEmailIgnoreCase(principal.getUsername())
				.filter(account -> account.isActive())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Zaloguj się ponownie"));
		return userMapper.toResponse(user);
	}

	@PostMapping("/me/deletion-request")
	public MessageResponse requestDeletion(@AuthenticationPrincipal UserDetails principal,
			@Valid @RequestBody DeleteAccountRequest request) {
		accountService.requestDeletion(principal.getUsername(), request.password());
		return new MessageResponse("Wysłaliśmy link potwierdzający usunięcie konta. Sprawdź skrzynkę e-mail. "
				+ "Kolejną wiadomość możesz zamówić po minucie.");
	}
}
