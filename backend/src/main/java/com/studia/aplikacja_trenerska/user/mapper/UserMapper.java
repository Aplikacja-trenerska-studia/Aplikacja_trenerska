package com.studia.aplikacja_trenerska.user.mapper;

import org.springframework.stereotype.Component;

import com.studia.aplikacja_trenerska.user.dto.UserResponse;
import com.studia.aplikacja_trenerska.user.entity.User;

@Component
public class UserMapper {

	public UserResponse toResponse(User user) {
		return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getRole());
	}
}
