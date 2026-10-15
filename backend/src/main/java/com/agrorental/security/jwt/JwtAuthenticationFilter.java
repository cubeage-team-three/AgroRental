package com.agrorental.security.jwt;

import com.agrorental.admin.entity.Admin;
import com.agrorental.admin.repository.AdminRepository;
import com.agrorental.common.enums.Role;
import com.agrorental.farmer.entity.Farmer;
import com.agrorental.farmer.repository.FarmerRepository;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorStatus;
import com.agrorental.operator.repository.OperatorRepository;
import com.agrorental.partner.entity.Partner;
import com.agrorental.partner.repository.PartnerRepository;
import com.agrorental.security.principal.FarmerPrincipal;
import com.agrorental.security.principal.OperatorOnboardingPrincipal;
import com.agrorental.security.principal.OperatorPrincipal;
import com.agrorental.security.principal.PartnerPrincipal;
import com.agrorental.user.entity.User;
import com.agrorental.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

/**
 * Spring Security filter that intercepts HTTP requests, extracts and validates Bearer JWT tokens,
 * verifies operator and user active status against the database to protect against stale tokens,
 * and sets authentication details into the SecurityContextHolder.
 *
 * <p>Each account table has its own ID sequence, so a token's subject ID is resolved against exactly
 * the one table named by its account-type claim ({@link AccountType}). Tokens with a missing,
 * unsupported or role-inconsistent account type are never authenticated, and there is no
 * cross-table fallback.
 *
 * <p>Operator onboarding tokens (claim {@code scope=OPERATOR_ONBOARDING}, see
 * {@link JwtService#generateOperatorOnboardingToken}) are never session tokens. They are honoured only on
 * {@code POST /api/operators/{id}/documents} where {@code id} is the token subject and the operator is still
 * active and PENDING, and then only with ROLE_OPERATOR_ONBOARDING; on any other request they are ignored,
 * so the request is anonymous.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final RequestMatcher ONBOARDING_DOCUMENT_UPLOAD =
            PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/api/operators/{operatorId}/documents");

    private final JwtService jwtService;
    private final UserRepository userRepository;
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

                Long accountId = jwtService.extractUserId(token);
                String role = jwtService.extractRole(token);
                String mobile = jwtService.extractMobileNumber(token);
                AccountType accountType = AccountType.fromClaim(jwtService.extractAccountType(token));
                String scope = jwtService.extractScope(token);
                log.debug("JWT authentication attempt: method={}, path={}, accountId={}, role={}, accountType={}, mobile={}",
                        request.getMethod(), request.getRequestURI(), accountId, role, accountType, mobile);

                if (scope != null) {
                    // Restricted-purpose token: never a session, whatever its other claims say
                    authenticateOperatorOnboarding(accountId, role, accountType, scope, request);
                } else if (accountId == null || accountType == null || !accountType.permitsRole(role)) {
                    log.warn("JWT rejected: missing/unsupported account type or role mismatch (role={}, accountType={})",
                            role, accountType);
                } else {
                    switch (accountType) {
                        case OPERATOR -> authenticateOperator(accountId, request);
                        case PARTNER -> authenticatePartner(accountId, request);
                        case FARMER -> authenticateFarmer(accountId, request);
                        case ADMIN -> authenticateAdmin(accountId, request);
                        case USER -> authenticateCoreUserAdmin(accountId, request);
                    }
                }
            }
        } catch (Exception ex) {
            log.debug("Could not set user authentication in security context: {}", ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /** operators table only */
    private void authenticateOperator(Long operatorId, HttpServletRequest request) {
        Optional<Operator> operatorOpt = operatorRepository.findById(operatorId);
        if (operatorOpt.isEmpty()) {
            log.warn("Operator ID {} from token not found in database", operatorId);
            return;
        }
        Operator operator = operatorOpt.get();
        // Only authenticated if active and approved
        if (!operator.isActive() || operator.getStatus() != OperatorStatus.APPROVED) {
            log.warn("Operator ID {} token rejected: active={}, status={}",
                    operatorId, operator.isActive(), operator.getStatus());
            return;
        }
        OperatorPrincipal principal = OperatorPrincipal.builder()
                .id(operator.getId())
                .mobileNumber(operator.getMobileNumber())
                .fullName(operator.getFullName())
                .role("OPERATOR")
                .build();
        setAuthentication(principal, "ROLE_OPERATOR", request);
        log.debug("Authenticated operator ID {} in SecurityContext", operator.getId());
    }

    /**
     * Onboarding token: only for the subject's own document upload while the operator is active and PENDING.
     * Anything else leaves the request unauthenticated.
     */
    private void authenticateOperatorOnboarding(Long operatorId, String role, AccountType accountType, String scope,
                                                HttpServletRequest request) {
        if (!JwtService.OPERATOR_ONBOARDING_SCOPE.equals(scope)
                || !JwtService.OPERATOR_ONBOARDING_ROLE.equals(role)
                || accountType != AccountType.OPERATOR
                || operatorId == null) {
            log.warn("JWT rejected: unsupported scoped token (scope={}, role={}, accountType={})", scope, role, accountType);
            return;
        }
        RequestMatcher.MatchResult match = ONBOARDING_DOCUMENT_UPLOAD.matcher(request);
        if (!match.isMatch() || !String.valueOf(operatorId).equals(match.getVariables().get("operatorId"))) {
            log.debug("Onboarding token ignored outside its own document upload: {} {}", request.getMethod(), request.getRequestURI());
            return;
        }
        Optional<Operator> operatorOpt = operatorRepository.findById(operatorId);
        if (operatorOpt.isEmpty() || !operatorOpt.get().isActive() || operatorOpt.get().getStatus() != OperatorStatus.PENDING) {
            log.warn("Onboarding token for operator ID {} rejected: operator missing, inactive or no longer PENDING", operatorId);
            return;
        }
        setAuthentication(new OperatorOnboardingPrincipal(operatorId), OperatorOnboardingPrincipal.AUTHORITY, request);
        log.debug("Authenticated onboarding operator ID {} for its document upload", operatorId);
    }

    /** partners table only */
    private void authenticatePartner(Long partnerId, HttpServletRequest request) {
        Optional<Partner> partnerOpt = partnerRepository.findById(partnerId);
        if (partnerOpt.isEmpty() || !partnerOpt.get().isActive()
                || partnerOpt.get().getVerificationStatus() != Partner.VerificationStatus.APPROVED) {
            log.warn("Partner ID {} token rejected: not found, inactive, or not approved", partnerId);
            return;
        }
        Partner partner = partnerOpt.get();
        PartnerPrincipal principal = PartnerPrincipal.builder()
                .id(partner.getId())
                .mobileNumber(partner.getMobileNumber())
                .fullName(partner.getFullName())
                .role("PARTNER")
                .build();
        setAuthentication(principal, "ROLE_PARTNER", request);
        log.debug("Authenticated partner ID {} in SecurityContext", partnerId);
    }

    /** farmers table only (email and mobile logins both issue farmers.id) */
    private void authenticateFarmer(Long farmerId, HttpServletRequest request) {
        Optional<Farmer> farmerOpt = farmerRepository.findById(farmerId);
        if (farmerOpt.isEmpty() || !isFarmerLoginable(farmerOpt.get())) {
            log.warn("Farmer ID {} token rejected: not found or account not active", farmerId);
            return;
        }
        Farmer farmer = farmerOpt.get();
        FarmerPrincipal principal = FarmerPrincipal.builder()
                .id(farmer.getFarmerId())
                .mobileNumber(farmer.getMobileNumber())
                .fullName(farmer.getFullName())
                .role("FARMER")
                .build();
        setAuthentication(principal, "ROLE_FARMER", request);
        log.debug("Authenticated farmer ID {} in SecurityContext", farmerId);
    }

    /** admins table only */
    private void authenticateAdmin(Long adminId, HttpServletRequest request) {
        Optional<Admin> adminOpt = adminRepository.findById(adminId);
        if (adminOpt.isEmpty() || !adminOpt.get().isActive()) {
            log.warn("Admin ID {} token rejected: not found or inactive", adminId);
            return;
        }
        setAuthentication("ADMIN_" + adminId, "ROLE_ADMIN", request);
        log.debug("Authenticated admin ID {} in SecurityContext", adminId);
    }

    /** users table only, and only for core users whose own role is ADMIN */
    private void authenticateCoreUserAdmin(Long userId, HttpServletRequest request) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty() || !userOpt.get().isEnabled() || userOpt.get().getRole() != Role.ADMIN) {
            log.warn("Core user ID {} admin token rejected: not found, disabled, or not an ADMIN", userId);
            return;
        }
        setAuthentication("ADMIN_USER_" + userId, "ROLE_ADMIN", request);
        log.debug("Authenticated core user admin ID {} in SecurityContext", userId);
    }

    private void setAuthentication(Object principal, String authority, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                Collections.singletonList(new SimpleGrantedAuthority(authority))
        );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /** PENDING_OTP no longer blocks a session (it only meant "OTP not done"); deactivated farmers stay blocked. */
    private boolean isFarmerLoginable(Farmer farmer) {
        return farmer.isActive() && !"INACTIVE".equalsIgnoreCase(farmer.getAccountStatus());
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (bearerToken != null && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length()).trim();
        }
        return null;
    }
}
