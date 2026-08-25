package com.agrorental.security.jwt;

import com.agrorental.admin.entity.Admin;
import com.agrorental.admin.repository.AdminRepository;
import com.agrorental.farmer.entity.Farmer;
import com.agrorental.farmer.repository.FarmerRepository;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorStatus;
import com.agrorental.operator.repository.OperatorRepository;
import com.agrorental.partner.entity.Partner;
import com.agrorental.partner.repository.PartnerRepository;
import com.agrorental.security.principal.FarmerPrincipal;
import com.agrorental.security.principal.OperatorPrincipal;
import com.agrorental.security.principal.PartnerPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

/**
 * Spring Security filter that intercepts HTTP requests, extracts and validates Bearer JWT tokens,
 * verifies operator active and approval state against the database to protect against stale tokens,
 * and sets the authenticated OperatorPrincipal into the SecurityContextHolder.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final OperatorRepository operatorRepository;
    private final AdminRepository adminRepository;
    private final PartnerRepository partnerRepository;
    private final FarmerRepository farmerRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = resolveToken(request);

            if (token != null && jwtService.validateToken(token)
                    && SecurityContextHolder.getContext().getAuthentication() == null) {

                Long userId = jwtService.extractUserId(token);
                String role = jwtService.extractRole(token);
                String mobile = jwtService.extractMobileNumber(token);

                if (userId != null && "OPERATOR".equalsIgnoreCase(role)) {
                    // Database validation against stale/deactivated tokens
                    Optional<Operator> operatorOpt = operatorRepository.findById(userId);

                    if (operatorOpt.isPresent()) {
                        Operator operator = operatorOpt.get();

                        // Only authenticated if active and approved
                        if (operator.isActive() && operator.getStatus() == OperatorStatus.APPROVED) {
                            OperatorPrincipal principal = OperatorPrincipal.builder()
                                    .id(operator.getId())
                                    .mobileNumber(operator.getMobileNumber())
                                    .fullName(operator.getFullName())
                                    .role("OPERATOR")
                                    .build();

                            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                    principal,
                                    null,
                                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_OPERATOR"))
                            );

                            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                            log.debug("Authenticated operator ID {} in SecurityContext", operator.getId());
                        } else {
                            log.warn("Operator ID {} token rejected: active={}, status={}",
                                    userId, operator.isActive(), operator.getStatus());
                        }
                    } else {
                        log.warn("Operator ID {} from token not found in database", userId);
                    }
                } else if (userId != null && "ADMIN".equalsIgnoreCase(role)) {
                    // Database validation against stale/deactivated tokens
                    Optional<Admin> adminOpt = adminRepository.findById(userId);

                    if (adminOpt.isPresent() && adminOpt.get().isActive()) {
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                "ADMIN_" + userId,
                                null,
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
                        );
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        log.debug("Authenticated admin ID {} in SecurityContext", userId);
                    } else {
                        log.warn("Admin ID {} token rejected: not found or inactive", userId);
                    }
                } else if (userId != null && "PARTNER".equalsIgnoreCase(role)) {
                    // Database validation against stale/deactivated tokens
                    Optional<Partner> partnerOpt = partnerRepository.findById(userId);

                    if (partnerOpt.isPresent() && partnerOpt.get().isActive()) {
                        Partner partner = partnerOpt.get();
                        PartnerPrincipal principal = PartnerPrincipal.builder()
                                .id(partner.getId())
                                .mobileNumber(partner.getMobileNumber())
                                .fullName(partner.getFullName())
                                .role("PARTNER")
                                .build();

                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_PARTNER"))
                        );
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        log.debug("Authenticated partner ID {} in SecurityContext", userId);
                    } else {
                        log.warn("Partner ID {} token rejected: not found or inactive", userId);
                    }
                } else if (userId != null && "FARMER".equalsIgnoreCase(role)) {
                    // Database validation against stale/deactivated tokens
                    Optional<Farmer> farmerOpt = farmerRepository.findById(userId);

                    if (farmerOpt.isPresent() && isFarmerLoginable(farmerOpt.get())) {
                        Farmer farmer = farmerOpt.get();
                        FarmerPrincipal principal = FarmerPrincipal.builder()
                                .id(farmer.getFarmerId())
                                .mobileNumber(farmer.getMobileNumber())
                                .fullName(farmer.getFullName())
                                .role("FARMER")
                                .build();

                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_FARMER"))
                        );
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        log.debug("Authenticated farmer ID {} in SecurityContext", userId);
                    } else {
                        log.warn("Farmer ID {} token rejected: not found or account not active", userId);
                    }
                }
            }
        } catch (Exception ex) {
            log.debug("Could not set user authentication in security context: {}", ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private boolean isFarmerLoginable(Farmer farmer) {
        String status = farmer.getAccountStatus();
        return farmer.isActive()
                && !"PENDING_OTP".equalsIgnoreCase(status)
                && !"INACTIVE".equalsIgnoreCase(status);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (bearerToken != null && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length()).trim();
        }
        return null;
    }
}
