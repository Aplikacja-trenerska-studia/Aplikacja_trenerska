package com.studia.aplikacja_trenerska.common.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<Map<String, String>> handleUnreadableBody() {
		return ResponseEntity.badRequest().body(Map.of("message", "Nieprawidłowa treść żądania JSON"));
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<Map<String, Object>> handleInvalidParameter(MethodArgumentTypeMismatchException ex) {
		return ResponseEntity.badRequest().body(Map.of(
				"message", "Nieprawidłowa wartość parametru: " + ex.getName(),
				"errors", Map.of(ex.getName(), "Nieprawidłowa wartość")));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
		Map<String, String> fieldErrors = new HashMap<>();
		for (FieldError error : ex.getBindingResult().getFieldErrors()) {
			fieldErrors.put(error.getField(), error.getDefaultMessage());
		}
		return ResponseEntity.badRequest().body(Map.of(
				"message", "Błąd walidacji",
				"errors", fieldErrors));
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<Map<String, String>> handleResponseStatus(ResponseStatusException ex) {
		HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
		String message = ex.getReason() != null ? ex.getReason() : "Błąd żądania";
		return ResponseEntity.status(status != null ? status : HttpStatus.BAD_REQUEST)
				.body(Map.of("message", message));
	}

	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<Map<String, String>> handleBadCredentials() {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(Map.of("message", "Nieprawidłowy e-mail lub hasło"));
	}

	@ExceptionHandler(DisabledException.class)
	public ResponseEntity<Map<String, String>> handleDisabledAccount() {
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(Map.of("message", "Potwierdź adres e-mail, aby aktywować konto. Możesz poprosić o nowy link."));
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<Map<String, String>> handleConflict() {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(Map.of("message", "Nie można zapisać danych. Konto z tym adresem może już istnieć."));
	}
}
