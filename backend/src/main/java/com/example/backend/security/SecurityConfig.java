package com.example.backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults()) // Enables CORS so Next.js can talk to Spring Boot
                .csrf(csrf -> csrf.disable())    // Disables CSRF protection so POST requests work
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/vault/**").permitAll() // Opens our specific endpoints
                        .requestMatchers("/api/subscribers/**").permitAll() // Add this line
                        .requestMatchers("/api/segments/**").permitAll() // Add this line
                        .requestMatchers("/api/users/**").permitAll() // ADD THIS LINE
                        .anyRequest().authenticated()
                );
        return http.build();
    }
}