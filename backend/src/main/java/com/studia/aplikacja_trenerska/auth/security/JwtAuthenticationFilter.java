package com.studia.aplikacja_trenerska.auth.security;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.studia.aplikacja_trenerska.auth.service.JwtService;
import com.studia.aplikacja_trenerska.user.repository.UserRepository;
import io.jsonwebtoken.JwtException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtService jwtService;
	private final UserRepository userRepository;

	public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
		this.jwtService = jwtService;
		this.userRepository = userRepository;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (request.getRequestURI().startsWith("/api/")) {
			response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
		}
		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
		}

		String jwt = authHeader.substring(7);
		try {
			var claims = jwtService.parseToken(jwt);
			Number userId = claims.get("userId", Number.class);
			Number version = claims.get("version", Number.class);
			if (claims.getSubject() != null && userId != null && version != null
					&& SecurityContextHolder.getContext().getAuthentication() == null) {
				userRepository.findByEmailIgnoreCase(claims.getSubject())
						.filter(user -> user.isEnabled() && user.getId().longValue() == userId.longValue()
								&& user.getTokenVersion() == version.longValue())
						.ifPresent(user -> {
							var details = org.springframework.security.core.userdetails.User.builder()
									.username(user.getEmail()).password(user.getPasswordHash())
									.authorities("ROLE_" + user.getRole().name()).build();
							var authentication = new UsernamePasswordAuthenticationToken(
									details, null, details.getAuthorities());
							authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
							SecurityContextHolder.getContext().setAuthentication(authentication);
						});
			}
		} catch (JwtException | IllegalArgumentException ex) {
			SecurityContextHolder.clearContext();
		}

		filterChain.doFilter(request, response);
	}
}
