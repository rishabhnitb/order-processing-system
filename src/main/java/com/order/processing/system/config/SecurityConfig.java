package com.order.processing.system.config;

import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final ApplicationEventPublisher eventPublisher;

    public SecurityConfig(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
        // Mark application as LIVE and READY when security config is initialized
        AvailabilityChangeEvent.publish(eventPublisher, this, LivenessState.CORRECT);
        AvailabilityChangeEvent.publish(eventPublisher, this, ReadinessState.ACCEPTING_TRAFFIC);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers("/actuator/**", "/swagger-ui/**", "/v3/api-docs/**"))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/**", "/actuator/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .anyRequest().authenticated());

        return http.build();
    }
}
