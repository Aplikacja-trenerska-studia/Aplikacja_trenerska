package com.studia.aplikacja_trenerska.user.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.studia.aplikacja_trenerska.user.dto.UserResponse;
import com.studia.aplikacja_trenerska.user.mapper.UserMapper;
import com.studia.aplikacja_trenerska.user.repository.UserRepository;

@RestController
@RequestMapping("/api/users")
public class MeController {

	private final UserRepository userRepository;
	private final UserMapper userMapper;

	public MeController(UserRepository userRepository, UserMapper userMapper) {
		this.userRepository = userRepository;
		this.userMapper = userMapper;
	}

	@GetMapping("/me")
	public UserResponse me(@AuthenticationPrincipal UserDetails principal) {
		var user = userRepository.findByEmailIgnoreCase(principal.getUsername()).orElseThrow();
		return userMapper.toResponse(user);
	}
}
