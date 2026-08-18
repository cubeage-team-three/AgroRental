package com.agrorental.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

<<<<<<< HEAD
/**
 * Central Spring Security configuration for AgroRental backend.
 * Configures HTTP security authorization rules, stateless session management, and CORS.
 */
=======
>>>>>>> origin/development
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
<<<<<<< HEAD
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/**", "/h2-console/**", "/**").permitAll()
                .anyRequest().permitAll()
            )
            .headers(headers -> headers.frameOptions(frame -> frame.disable()));
=======
            .cors(cors ->
                cors.configurationSource(corsConfigurationSource())
            )

           .csrf(csrf -> csrf.disable())

            .sessionManagement(session ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            )

            .authorizeHttpRequests(auth -> auth

                .requestMatchers(HttpMethod.OPTIONS, "/**")
                .permitAll()

                .requestMatchers(
                    "/api/partners/register",
                    "/api/users/**",
                    "/api/auth/**",
                    "/api/farmers/**"
                )
                .permitAll()

                .requestMatchers("/api/equipment/**")
                .permitAll()

                .requestMatchers("/api/**")
                .permitAll()

                .requestMatchers("/h2-console/**")
                .permitAll()

                .anyRequest()
                .permitAll()
            )

            .headers(headers ->
                headers.frameOptions(frame ->
                    frame.disable()
                )
            );
>>>>>>> origin/development

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
<<<<<<< HEAD
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
=======

        CorsConfiguration configuration =
                new CorsConfiguration();

        configuration.setAllowedOrigins(List.of(
            "http://localhost:5173",
            "http://localhost:5174",
            "http://localhost:5175",
            "http://localhost:5176",
            "http://localhost:5177",
            "http://localhost:5178"
        ));

        configuration.setAllowedMethods(List.of(
            "GET",
            "POST",
            "PUT",
            "PATCH",
            "DELETE",
            "OPTIONS"
        ));

        configuration.setAllowedHeaders(List.of("*"));

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
            "/**",
            configuration
        );

>>>>>>> origin/development
        return source;
    }
}