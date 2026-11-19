package com.studia.aplikacja_trenerska.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.studia.aplikacja_trenerska.auth.security.JwtAuthenticationFilter;
import com.studia.aplikacja_trenerska.auth.service.AppUserDetailsService;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final AppUserDetailsService userDetailsService;
	private final AccountProperties accountProperties;
	private final CorsProperties corsProperties;

	public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, AppUserDetailsService userDetailsService,
			AccountProperties accountProperties, CorsProperties corsProperties) {
		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
		this.userDetailsService = userDetailsService;
		this.accountProperties = accountProperties;
		this.corsProperties = corsProperties;
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
				.csrf(AbstractHttpConfigurer::disable)
				.cors(cors -> cors.configurationSource(corsConfigurationSource()))
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(errors -> errors
						.authenticationEntryPoint((request, response, ex) -> {
							response.setStatus(401);
							response.setContentType("application/json;charset=UTF-8");
							response.getWriter().write("{\"message\":\"Zaloguj się, aby kontynuować\"}");
						})
						.accessDeniedHandler((request, response, ex) -> {
							response.setStatus(403);
							response.setContentType("application/json;charset=UTF-8");
							response.getWriter().write("{\"message\":\"Brak uprawnień\"}");
						}))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.GET, "/api/health").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login",
								"/api/auth/confirm", "/api/auth/resend-confirmation", "/api/auth/forgot-password",
								"/api/auth/reset-password", "/api/auth/confirm-deletion").permitAll()
						.requestMatchers("/swagger-ui/**", "/api-docs/**", "/v3/api-docs/**").permitAll()
						.requestMatchers("/api/admin", "/api/admin/**").hasRole("ADMIN")
						.requestMatchers("/api/trainer", "/api/trainer/**", "/api/groups", "/api/groups/**")
						.hasRole("TRAINER")
						.requestMatchers("/api/**").authenticated()
						.requestMatchers("/error").permitAll()
						.anyRequest().denyAll())
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
				.build();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		var origins = new java.util.LinkedHashSet<>(corsProperties.allowedOrigins());
		origins.add(CorsProperties.normalizeOrigin(accountProperties.frontendUrl()));
		configuration.setAllowedOrigins(java.util.List.copyOf(origins));
		configuration.setAllowedOriginPatterns(corsProperties.allowedOriginPatterns());
		configuration.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(java.util.List.of("*"));
		configuration.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	AuthenticationManager authenticationManager(PasswordEncoder passwordEncoder) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder);
		return new ProviderManager(provider);
	}
}
