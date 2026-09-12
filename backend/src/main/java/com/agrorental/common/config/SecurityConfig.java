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
                        // Farmer Review submission on completed operator job
                        .requestMatchers(HttpMethod.POST, "/api/operators/jobs/*/reviews").hasRole("FARMER")
                        .requestMatchers(HttpMethod.GET, "/api/operators/jobs/*/review").authenticated()
                        // Operator Protected Endpoints
                        .requestMatchers("/api/operators/me").hasRole("OPERATOR")
                        .requestMatchers("/api/operators/me/**").hasRole("OPERATOR")
                        .requestMatchers("/api/operators/profile/**").hasRole("OPERATOR")
                        .requestMatchers("/api/operators/jobs/**").hasRole("OPERATOR")
                        .requestMatchers("/api/operators/dashboard/**").hasRole("OPERATOR")
                        .requestMatchers("/api/operators/earnings/**").hasRole("OPERATOR")
                        // Shared / genuinely public endpoints
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/users/**").permitAll()
                        .requestMatchers("/uploads/**").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        // Farmer: registration + OTP are public; farms, profile, dashboard, and management require authentication
                        .requestMatchers(HttpMethod.POST, "/api/farmers/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/farmers/send-otp", "/api/farmers/verify-otp", "/api/farmers/resend-otp").permitAll()
                        .requestMatchers("/api/farmers", "/api/farmers/**", "/api/farmers/farms", "/api/farmers/farms/**").hasRole("FARMER")

                        // Partner: registration + OTP are public; Admin manages partners & KYC; Partner manages own profile/dashboard
                        .requestMatchers(HttpMethod.POST, "/api/partners/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/partners/*/otp/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/partners", "/api/partners/").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/partners/*/kyc/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/partners/*/dashboard").hasAnyRole("ADMIN", "PARTNER")
                        .requestMatchers(HttpMethod.GET, "/api/partners/*").hasAnyRole("ADMIN", "PARTNER")
                        .requestMatchers("/api/partners/**").hasRole("PARTNER")

                        // Equipment: browsing/search is public, creation is partner-only, mutations allow partner and admin
                        .requestMatchers(HttpMethod.GET, "/api/equipment", "/api/equipment/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/equipment").hasRole("PARTNER")
                        .requestMatchers("/api/equipment", "/api/equipment/**").hasAnyRole("PARTNER", "ADMIN")
                        // Image Uploads: static file retrieval is public, uploading requires PARTNER role
                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                        .requestMatchers("/api/upload/**").hasRole("PARTNER")
                        // Bookings: role-specific views and actions, ownership resolved from the JWT in-controller
                        .requestMatchers(HttpMethod.POST, "/api/bookings").hasRole("FARMER")
                        .requestMatchers(HttpMethod.GET, "/api/bookings/farmer/**").hasRole("FARMER")
                        .requestMatchers(HttpMethod.GET, "/api/bookings/partner/**").hasAnyRole("ADMIN", "PARTNER")
                        .requestMatchers(HttpMethod.GET, "/api/bookings/operator/**").hasRole("OPERATOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/bookings/*/cancel").hasRole("FARMER")
                        .requestMatchers(HttpMethod.PATCH, "/api/bookings/*/accept", "/api/bookings/*/reject", "/api/bookings/*/assign-operator").hasRole("PARTNER")
                        .requestMatchers("/api/bookings/**").authenticated()
                        // Payments: partner earnings/reports vs. farmer-facing payment actions
                        .requestMatchers("/api/payments/partner/**").hasRole("PARTNER")
                        .requestMatchers("/api/payments/farmer/**").hasRole("FARMER")
                        .requestMatchers("/api/payments/**").authenticated()
                        // Reviews: reading ratings is public, submitting one requires a farmer login
                        .requestMatchers(HttpMethod.GET, "/api/reviews/**").permitAll()
                        .requestMatchers("/api/reviews/**").hasRole("FARMER")
                        // Notifications: any authenticated role reads/manages their own
                        .requestMatchers("/api/notifications/**").authenticated()
                        .anyRequest().authenticated())
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
