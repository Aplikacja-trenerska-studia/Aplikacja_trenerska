package com.studia.aplikacja_trenerska.user.dto;

import com.studia.aplikacja_trenerska.user.entity.UserRole;

public record UserResponse(
		Long id,
		String email,
		String displayName,
		UserRole role
) {
}
