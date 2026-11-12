package com.studia.aplikacja_trenerska.auth.dto;

import com.studia.aplikacja_trenerska.user.dto.UserResponse;

public record AuthResponse(
		String accessToken,
		String tokenType,
		long expiresInMs,
		UserResponse user
) {
}
