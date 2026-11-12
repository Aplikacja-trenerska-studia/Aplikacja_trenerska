package com.studia.aplikacja_trenerska.auth.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import com.studia.aplikacja_trenerska.config.JwtProperties;
import com.studia.aplikacja_trenerska.user.entity.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

	private final JwtProperties jwtProperties;
	private final SecretKey signingKey;

	public JwtService(JwtProperties jwtProperties) {
		this.jwtProperties = jwtProperties;
		this.signingKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
	}

	public String generateToken(User user) {
		Instant now = Instant.now();
		Instant expiry = now.plusMillis(jwtProperties.expirationMs());

		return Jwts.builder()
				.subject(user.getEmail())
				.claim("role", user.getRole().name())
				.claim("userId", user.getId())
				.issuedAt(Date.from(now))
				.expiration(Date.from(expiry))
				.signWith(signingKey)
				.compact();
	}

	public Claims parseToken(String token) {
		return Jwts.parser()
				.verifyWith(signingKey)
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}

	public String extractEmail(String token) {
		return parseToken(token).getSubject();
	}

	public boolean isTokenValid(String token) {
		try {
			Claims claims = parseToken(token);
			return claims.getExpiration().after(new Date());
		}
		catch (RuntimeException ex) {
			return false;
		}
	}

	public long getExpirationMs() {
		return jwtProperties.expirationMs();
	}
}
