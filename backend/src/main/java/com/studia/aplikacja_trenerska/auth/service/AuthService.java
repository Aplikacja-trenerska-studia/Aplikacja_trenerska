package com.studia.aplikacja_trenerska.auth.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.studia.aplikacja_trenerska.auth.dto.AuthResponse;
import com.studia.aplikacja_trenerska.auth.dto.LoginRequest;
import com.studia.aplikacja_trenerska.auth.dto.RegisterRequest;
import com.studia.aplikacja_trenerska.user.entity.User;
import com.studia.aplikacja_trenerska.user.entity.UserRole;
import com.studia.aplikacja_trenerska.user.mapper.UserMapper;
import com.studia.aplikacja_trenerska.user.repository.UserRepository;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;
	private final UserMapper userMapper;

	public AuthService(
			UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			AuthenticationManager authenticationManager,
			JwtService jwtService,
			UserMapper userMapper
	) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.authenticationManager = authenticationManager;
		this.jwtService = jwtService;
		this.userMapper = userMapper;
	}

	@Transactional
	public AuthResponse register(RegisterRequest request) {
		String email = request.email().trim().toLowerCase();
		if (userRepository.existsByEmailIgnoreCase(email)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Konto z tym adresem e-mail już istnieje");
		}

		User user = new User();
		user.setEmail(email);
		user.setPasswordHash(passwordEncoder.encode(request.password()));
		user.setDisplayName(request.displayName().trim());
		user.setRole(UserRole.USER);
		user.setEnabled(true);

		User saved = userRepository.save(user);
		return buildAuthResponse(saved);
	}

	public AuthResponse login(LoginRequest request) {
		String email = request.email().trim().toLowerCase();
		authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(email, request.password()));

		User user = userRepository.findByEmailIgnoreCase(email)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Nieprawidłowe dane logowania"));

		return buildAuthResponse(user);
	}

	private AuthResponse buildAuthResponse(User user) {
		String token = jwtService.generateToken(user);
		return new AuthResponse(token, "Bearer", jwtService.getExpirationMs(), userMapper.toResponse(user));
	}
}
