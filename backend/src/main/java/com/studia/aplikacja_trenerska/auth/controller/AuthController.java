package com.studia.aplikacja_trenerska.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.studia.aplikacja_trenerska.auth.dto.AuthResponse;
import com.studia.aplikacja_trenerska.auth.dto.LoginRequest;
import com.studia.aplikacja_trenerska.auth.dto.RegisterRequest;
import com.studia.aplikacja_trenerska.auth.dto.EmailRequest;
import com.studia.aplikacja_trenerska.auth.dto.MessageResponse;
import com.studia.aplikacja_trenerska.auth.dto.ResetPasswordRequest;
import com.studia.aplikacja_trenerska.auth.dto.TokenRequest;
import com.studia.aplikacja_trenerska.auth.service.AccountService;
import com.studia.aplikacja_trenerska.auth.service.AuthService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;
	private final AccountService accountService;

	public AuthController(AuthService authService, AccountService accountService) {
		this.authService = authService;
		this.accountService = accountService;
	}

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	public MessageResponse register(@Valid @RequestBody RegisterRequest request) {
		return authService.register(request);
	}

	@PostMapping("/login")
	public AuthResponse login(@Valid @RequestBody LoginRequest request) {
		return authService.login(request);
	}

	@PostMapping("/forgot-password")
	public MessageResponse forgotPassword(@Valid @RequestBody EmailRequest request) {
		requestMailPrivately(() -> accountService.forgotPassword(request.email()));
		return new MessageResponse("Jeśli istnieje aktywne konto z tym adresem, wyślemy link do resetu hasła. "
				+ "Kolejną wiadomość możesz zamówić po minucie.");
	}

	@PostMapping("/reset-password")
	public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
		accountService.resetPassword(request.token(), request.password());
		return new MessageResponse("Hasło zostało zmienione. Zaloguj się nowym hasłem.");
	}

	@PostMapping("/confirm")
	public MessageResponse confirm(@Valid @RequestBody TokenRequest request) {
		accountService.confirmEmail(request.token());
		return new MessageResponse("Konto zostało aktywowane. Możesz się zalogować.");
	}

	@PostMapping("/resend-confirmation")
	public MessageResponse resendConfirmation(@Valid @RequestBody EmailRequest request) {
		requestMailPrivately(() -> accountService.resendConfirmation(request.email()));
		return new MessageResponse("Jeśli konto oczekuje na aktywację, wyślemy nowy link potwierdzający. "
				+ "Kolejną wiadomość możesz zamówić po minucie.");
	}

	@PostMapping("/confirm-deletion")
	public MessageResponse confirmDeletion(@Valid @RequestBody TokenRequest request) {
		accountService.confirmDeletion(request.token());
		return new MessageResponse("Konto i powiązane dane zostały usunięte.");
	}

	private void requestMailPrivately(Runnable request) {
		try {
			request.run();
		} catch (ResponseStatusException ex) {
			// AccountService has already rolled back. SMTP outages must not disclose account existence.
			if (ex.getStatusCode().value() != HttpStatus.SERVICE_UNAVAILABLE.value()) {
				throw ex;
			}
		}
	}
}
