package com.agrorental.common.config;

import com.agrorental.security.jwt.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Central Spring Security configuration for AgroRental backend.
 * Configures HTTP security authorization rules, stateless session management,
 * JWT authentication filter, CORS, and role-based endpoint permissions.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setContentType("application/json;charset=UTF-8");
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.getWriter().write("{\"success\":false,\"message\":\"Full authentication is required to access this resource\",\"data\":null}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setContentType("application/json;charset=UTF-8");
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.getWriter().write("{\"success\":false,\"message\":\"Access is denied. You do not have the required permissions.\",\"data\":null}");
                        })
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Public Operator Endpoints (Registration, OTP, Login)
                        .requestMatchers(HttpMethod.POST, "/api/operators/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/operators/login").permitAll()
                        .requestMatchers("/api/operators/otp/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/operators/*/documents").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/operators/*/documents").permitAll()
                        // Operator Protected Endpoints
                        .requestMatchers("/api/operators/me").hasRole("OPERATOR")
                        .requestMatchers("/api/operators/profile/**").hasRole("OPERATOR")
                        .requestMatchers("/api/operators/jobs/**").hasRole("OPERATOR")
                        .requestMatchers("/api/operators/dashboard/**").hasRole("OPERATOR")
                        // Admin Protected Endpoints
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // Existing Platform Public and Shared Endpoints
                        .requestMatchers(
                                "/api/partners/register",
                                "/api/users/**",
                                "/api/auth/**",
                                "/api/farmers/**",
                                "/api/bookings/**",
                                "/api/equipment/**",
                                "/api/**",
                                "/h2-console/**")
                        .permitAll()
                        .anyRequest().permitAll())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .headers(headers -> headers.frameOptions(frame -> frame.disable()));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
