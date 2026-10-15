package com.agrorental.common.config;

import com.agrorental.security.jwt.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
    private final CorsProperties corsProperties;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, Environment environment) throws Exception {
        // The H2 console is opened only when it is actually registered (explicit dev profile + property);
        // otherwise /h2-console/** falls through to the authenticated catch-all below.
        if (DeploymentProfiles.isH2ConsoleEnabled(environment)) {
            http.authorizeHttpRequests(auth -> auth.requestMatchers("/h2-console/**").permitAll());
        }

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
                        // Public Operator Endpoints (registration; the retired /login answers 410 Gone; OTP endpoints
                        // only record informational mobile verification and never issue tokens)
                        .requestMatchers(HttpMethod.POST, "/api/operators/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/operators/login").permitAll()
                        .requestMatchers("/api/operators/otp/**").permitAll()
                        // Operator KYC documents: never public. Reads are admin-only; uploads require the
                        // owning (approved) operator, an admin, or the operator's own registration onboarding token
                        // (ROLE_OPERATOR_ONBOARDING, granted by the JWT filter only on this route for the token's own
                        // operator ID while PENDING); ownership is enforced again in the controller.
                        .requestMatchers(HttpMethod.GET, "/api/operators/*/documents").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/operators/*/documents").hasAnyRole("OPERATOR", "ADMIN", "OPERATOR_ONBOARDING")
                        .requestMatchers("/api/operators/*/documents", "/api/operators/*/documents/**").denyAll()
                        // Operator directories expose personal data: platform-wide lists are admin-only; a partner may
                        // see its own roster and a minimized eligible-operator list (scoping enforced in the services)
                        .requestMatchers(HttpMethod.GET, "/api/operators", "/api/operators/available").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/operators/partner/*", "/api/operators/eligible").hasAnyRole("PARTNER", "ADMIN")
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
                        // Admin Protected Endpoints
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // Shared / genuinely public endpoints
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/users/**").permitAll()
                        .requestMatchers("/uploads/**").permitAll()
                        // Farmer: registration is public (OTP endpoints remain public but never issue a session);
                        // farms, profile, dashboard, and management require authentication
                        .requestMatchers(HttpMethod.POST, "/api/farmers/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/farmers/send-otp", "/api/farmers/verify-otp", "/api/farmers/resend-otp").permitAll()
                        .requestMatchers("/api/farmers", "/api/farmers/**", "/api/farmers/farms", "/api/farmers/farms/**").hasRole("FARMER")

                        // Partner: registration is public; KYC approval and the full partner
                        // listing are admin actions; a partner's own dashboard/detail is viewable
                        // by that partner or an admin, everything else is partner self-service
                        .requestMatchers(HttpMethod.POST, "/api/partners/register").permitAll()
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
                        // Operator assignment: partner (own bookings, enforced in service) or admin only
                        .requestMatchers(HttpMethod.POST, "/api/bookings/*/operator").hasAnyRole("PARTNER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/bookings/*/operator").hasAnyRole("PARTNER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/bookings/farmer/**").hasRole("FARMER")
                        .requestMatchers(HttpMethod.GET, "/api/bookings/partner/**").hasAnyRole("ADMIN", "PARTNER")
                        .requestMatchers(HttpMethod.GET, "/api/bookings/operator/**").hasRole("OPERATOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/bookings/*/cancel").hasRole("FARMER")
                        .requestMatchers(HttpMethod.PATCH, "/api/bookings/*/accept", "/api/bookings/*/reject", "/api/bookings/*/assign-operator").hasRole("PARTNER")
                        .requestMatchers("/api/bookings/**").authenticated()
                        // Payments: partner earnings/reports vs. farmer-facing payment actions
                        .requestMatchers("/api/payments/partner/**").hasRole("PARTNER")
                        .requestMatchers("/api/payments/farmer/**").hasRole("FARMER")
                        // General payment routes: creation by the booking's farmer (or admin); single-payment reads by
                        // the owning farmer/partner or admin (ownership enforced in controller and service). Never operators.
                        .requestMatchers(HttpMethod.POST, "/api/payments").hasAnyRole("FARMER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/payments/*", "/api/payments/booking/*").hasAnyRole("FARMER", "PARTNER", "ADMIN")
                        .requestMatchers("/api/payments/**").authenticated()
                        // Reviews: reading ratings is public, submitting one requires a farmer login
                        .requestMatchers(HttpMethod.GET, "/api/reviews/**").permitAll()
                        .requestMatchers("/api/reviews/**").hasRole("FARMER")
                        // Notifications: any authenticated role reads/manages their own
                        .requestMatchers("/api/notifications/**").authenticated()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // sameOrigin still allows the (same-origin) H2 console frames in explicit dev
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }

    /**
     * Authentication is exclusively by signed JWTs (JwtAuthenticationFilter) issued by POST /api/auth/login.
     * Declaring this bean stops Spring Boot from creating its default in-memory user with a generated
     * password (logged at startup); it never finds anyone, so no username/password login path exists.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException("Username/password authentication is not supported");
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.getAllowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization", "Content-Disposition"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
