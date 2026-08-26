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
import com.agrorental.common.enums.Role;
import com.agrorental.user.entity.User;
import com.agrorental.user.repository.UserRepository;

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

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final OperatorRepository operatorRepository;
    private final AdminRepository adminRepository;
    private final PartnerRepository partnerRepository;
    private final FarmerRepository farmerRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String token = resolveToken(request);

            if (token != null
                    && jwtService.validateToken(token)
                    && SecurityContextHolder.getContext().getAuthentication() == null) {

                Long userId = jwtService.extractUserId(token);
                String role = jwtService.extractRole(token);
                String mobile = jwtService.extractMobileNumber(token);

                log.info(
                        "JWT authentication: method={}, path={}, userId={}, role={}, mobile={}",
                        request.getMethod(),
                        request.getRequestURI(),
                        userId,
                        role,
                        mobile
                );

                if (userId == null || role == null) {
                    log.warn("JWT rejected: userId or role is missing");
                    filterChain.doFilter(request, response);
                    return;
                }

                /*
                 * ============================================================
                 * OPERATOR
                 * ============================================================
                 */
                if ("OPERATOR".equalsIgnoreCase(role)) {

                    Optional<Operator> operatorOpt =
                            operatorRepository.findById(userId);

                    if (operatorOpt.isPresent()) {

                        Operator operator = operatorOpt.get();

                        if (operator.isActive()
                                && operator.getStatus() == OperatorStatus.APPROVED) {

                            OperatorPrincipal principal =
                                    OperatorPrincipal.builder()
                                            .id(operator.getId())
                                            .mobileNumber(operator.getMobileNumber())
                                            .fullName(operator.getFullName())
                                            .role("OPERATOR")
                                            .build();

                            UsernamePasswordAuthenticationToken authentication =
                                    new UsernamePasswordAuthenticationToken(
                                            principal,
                                            null,
                                            Collections.singletonList(
                                                    new SimpleGrantedAuthority("ROLE_OPERATOR")
                                            )
                                    );

                            authentication.setDetails(
                                    new WebAuthenticationDetailsSource()
                                            .buildDetails(request)
                            );

                            SecurityContextHolder
                                    .getContext()
                                    .setAuthentication(authentication);

                            log.info(
                                    "Operator authenticated successfully: operatorId={}, authority=ROLE_OPERATOR",
                                    operator.getId()
                            );

                        } else {

                            log.warn(
                                    "Operator rejected: id={}, active={}, status={}",
                                    userId,
                                    operator.isActive(),
                                    operator.getStatus()
                            );
                        }

                    } else {

                        log.warn(
                                "Operator rejected: operatorId={} not found",
                                userId
                        );
                    }
                }

                /*
                 * ============================================================
                 * ADMIN
                 * ============================================================
                 */
                else if ("ADMIN".equalsIgnoreCase(role)) {

                    Optional<User> userOpt =
                            userRepository.findById(userId);

                    if (userOpt.isPresent()
                            && userOpt.get().isEnabled()
                            && userOpt.get().getRole() == Role.ADMIN) {

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        "ADMIN_" + userId,
                                        null,
                                        Collections.singletonList(
                                                new SimpleGrantedAuthority("ROLE_ADMIN")
                                        )
                                );

                        authentication.setDetails(
                                new WebAuthenticationDetailsSource()
                                        .buildDetails(request)
                        );

                        SecurityContextHolder
                                .getContext()
                                .setAuthentication(authentication);

                        log.info(
                                "Admin authenticated successfully: userId={}, authority=ROLE_ADMIN",
                                userId
                        );

                    } else {

                        Optional<Admin> adminOpt =
                                adminRepository.findById(userId);

                        if (adminOpt.isPresent()
                                && adminOpt.get().isActive()) {

                            UsernamePasswordAuthenticationToken authentication =
                                    new UsernamePasswordAuthenticationToken(
                                            "ADMIN_" + userId,
                                            null,
                                            Collections.singletonList(
                                                    new SimpleGrantedAuthority("ROLE_ADMIN")
                                            )
                                    );

                            authentication.setDetails(
                                    new WebAuthenticationDetailsSource()
                                            .buildDetails(request)
                            );

                            SecurityContextHolder
                                    .getContext()
                                    .setAuthentication(authentication);

                            log.info(
                                    "Legacy admin authenticated successfully: adminId={}, authority=ROLE_ADMIN",
                                    userId
                            );

                        } else {

                            log.warn(
                                    "Admin rejected: adminId={} not found or inactive",
                                    userId
                            );
                        }
                    }
                }

                /*
                 * ============================================================
                 * PARTNER
                 * ============================================================
                 */
                else if ("PARTNER".equalsIgnoreCase(role)) {

                    log.info(
                            "JWT Partner authentication: userId={}, role={}, mobile={}",
                            userId,
                            role,
                            mobile
                    );

                    Optional<Partner> partnerOpt =
                            partnerRepository.findById(userId);

                    if (partnerOpt.isPresent()) {

                        Partner partner = partnerOpt.get();

                        if (partner.isActive()) {

                            PartnerPrincipal principal =
                                    PartnerPrincipal.builder()
                                            .id(partner.getId())
                                            .mobileNumber(partner.getMobileNumber())
                                            .fullName(partner.getFullName())
                                            .role("PARTNER")
                                            .build();

                            UsernamePasswordAuthenticationToken authentication =
                                    new UsernamePasswordAuthenticationToken(
                                            principal,
                                            null,
                                            Collections.singletonList(
                                                    new SimpleGrantedAuthority("ROLE_PARTNER")
                                            )
                                    );

                            authentication.setDetails(
                                    new WebAuthenticationDetailsSource()
                                            .buildDetails(request)
                            );

                            SecurityContextHolder
                                    .getContext()
                                    .setAuthentication(authentication);

                            log.info(
                                    "Partner authenticated successfully: partnerId={}, authority=ROLE_PARTNER",
                                    partner.getId()
                            );

                        } else {

                            log.warn(
                                    "Partner rejected: partnerId={} is inactive",
                                    userId
                            );
                        }

                    } else {

                        log.warn(
                                "Partner rejected: partnerId={} not found in database",
                                userId
                        );
                    }
                }

                /*
                 * ============================================================
                 * FARMER
                 * ============================================================
                 */
                else if ("FARMER".equalsIgnoreCase(role)) {

                    Optional<User> userOpt =
                            userRepository != null ? userRepository.findById(userId) : Optional.empty();

                    if (userOpt.isPresent()
                            && userOpt.get().isEnabled()
                            && userOpt.get().getRole() == Role.FARMER) {

                        User user = userOpt.get();

                        Long farmerId =
                                user.getFarmer() != null
                                        ? user.getFarmer().getId()
                                        : user.getId();

                        FarmerPrincipal principal =
                                FarmerPrincipal.builder()
                                        .id(farmerId)
                                        .mobileNumber(user.getEmail())
                                        .fullName(user.getName())
                                        .role("FARMER")
                                        .build();

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        principal,
                                        null,
                                        Collections.singletonList(
                                                new SimpleGrantedAuthority("ROLE_FARMER")
                                        )
                                );

                        authentication.setDetails(
                                new WebAuthenticationDetailsSource()
                                        .buildDetails(request)
                        );

                        SecurityContextHolder
                                .getContext()
                                .setAuthentication(authentication);

                        log.info(
                                "Farmer authenticated successfully: userId={}, farmerId={}, authority=ROLE_FARMER",
                                user.getId(),
                                farmerId
                        );

                    } else {

                        /*
                         * Legacy Farmer fallback
                         */
                        Optional<Farmer> farmerOpt =
                                farmerRepository.findById(userId);

                        if (farmerOpt.isPresent()
                                && isFarmerLoginable(farmerOpt.get())) {

                            Farmer farmer = farmerOpt.get();

                            FarmerPrincipal principal =
                                    FarmerPrincipal.builder()
                                            .id(farmer.getFarmerId())
                                            .mobileNumber(farmer.getMobileNumber())
                                            .fullName(farmer.getFullName())
                                            .role("FARMER")
                                            .build();

                            UsernamePasswordAuthenticationToken authentication =
                                    new UsernamePasswordAuthenticationToken(
                                            principal,
                                            null,
                                            Collections.singletonList(
                                                    new SimpleGrantedAuthority("ROLE_FARMER")
                                            )
                                    );

                            authentication.setDetails(
                                    new WebAuthenticationDetailsSource()
                                            .buildDetails(request)
                            );

                            SecurityContextHolder
                                    .getContext()
                                    .setAuthentication(authentication);

                            log.info(
                                    "Legacy farmer authenticated successfully: farmerId={}, authority=ROLE_FARMER",
                                    farmer.getFarmerId()
                            );

                        } else {

                            log.warn(
                                    "Farmer rejected: farmerId={} not found or account inactive",
                                    userId
                            );
                        }
                    }
                }

                /*
                 * ============================================================
                 * UNKNOWN ROLE
                 * ============================================================
                 */
                else {

                    log.warn(
                            "JWT rejected: unsupported role={}",
                            role
                    );
                }
            }

        } catch (Exception ex) {

            log.error(
                    "JWT authentication failed for {} {}: {}",
                    request.getMethod(),
                    request.getRequestURI(),
                    ex.getMessage(),
                    ex
            );
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

        String bearerToken =
                request.getHeader(AUTHORIZATION_HEADER);

        if (bearerToken != null
                && bearerToken.startsWith(BEARER_PREFIX)) {

            return bearerToken
                    .substring(BEARER_PREFIX.length())
                    .trim();
        }

        return null;
    }
}