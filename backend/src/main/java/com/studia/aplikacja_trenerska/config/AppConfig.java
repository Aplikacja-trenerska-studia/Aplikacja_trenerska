package com.studia.aplikacja_trenerska.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({JwtProperties.class, AccountProperties.class, CorsProperties.class})
public class AppConfig {
}
