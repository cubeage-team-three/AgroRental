package com.agrorental.security;

import com.agrorental.common.exception.GlobalExceptionHandler;
import com.agrorental.operator.controller.OperatorReviewController;
import com.agrorental.operator.dto.OperatorRatingSummaryResponse;
import com.agrorental.operator.dto.OperatorReviewCreateRequest;
import com.agrorental.operator.dto.OperatorReviewResponse;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorStatus;
import com.agrorental.admin.repository.AdminRepository;
import com.agrorental.partner.repository.PartnerRepository;
import com.agrorental.farmer.repository.FarmerRepository;
import com.agrorental.operator.repository.OperatorRepository;
import com.agrorental.operator.service.OperatorReviewService;
import com.agrorental.security.jwt.JwtAuthenticationFilter;
import com.agrorental.security.jwt.JwtService;
import com.agrorental.security.principal.OperatorPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Operator Review Security & JWT Filter Integration Tests")
class OperatorReviewSecurityIntegrationTest {

    private MockMvc mockMvc;

    @Mock
    private JwtService jwtService;

    @Mock
    private OperatorRepository operatorRepository;

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private PartnerRepository partnerRepository;

    @Mock
    private FarmerRepository farmerRepository;

    @Mock
    private OperatorReviewService reviewService;

    @InjectMocks
    private OperatorReviewController reviewController;

    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtService, null, operatorRepository, adminRepository, partnerRepository, farmerRepository);

        HandlerMethodArgumentResolver principalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterType().isAssignableFrom(OperatorPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter,
                                          ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest,
                                          WebDataBinderFactory binderFactory) {
                var auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.getPrincipal() instanceof OperatorPrincipal op) {
                    return op;
                }
                return null;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(reviewController)
                .addFilters(jwtAuthenticationFilter)
                .setCustomArgumentResolvers(principalResolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Anonymous request to POST /api/operators/jobs/1/reviews returns 401 Unauthorized")
    void anonymousReview_returns401() throws Exception {
        String json = "{\"rating\":5,\"comment\":\"Great!\"}";

        mockMvc.perform(post("/api/operators/jobs/1/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Full authentication is required to access this resource")));
    }

    @Test
    @DisplayName("Anonymous request to GET /api/operators/me/ratings/summary returns 401 Unauthorized")
    void anonymousSummary_returns401() throws Exception {
        mockMvc.perform(get("/api/operators/me/ratings/summary")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Full authentication is required to access this resource")));
    }

    @Test
    @DisplayName("Valid Farmer JWT Bearer token submitting review returns 201 Created")
    void validFarmerToken_createsReview() throws Exception {
        String token = "valid.farmer.token";
        when(jwtService.validateToken(token)).thenReturn(true);
        when(jwtService.extractUserId(token)).thenReturn(10L);
        when(jwtService.extractRole(token)).thenReturn("FARMER");
        when(jwtService.extractMobileNumber(token)).thenReturn("9876500001");

        com.agrorental.farmer.entity.Farmer farmer = new com.agrorental.farmer.entity.Farmer();
        farmer.setId(10L);
        farmer.setActive(true);
        farmer.setAccountStatus("ACTIVE");
        when(farmerRepository.findById(10L)).thenReturn(Optional.of(farmer));

        String json = "{\"rating\":5,\"comment\":\"Great harvesting service!\"}";

        OperatorReviewResponse response = OperatorReviewResponse.builder()
                .reviewId(1L)
                .assignmentId(100L)
                .bookingId(200L)
                .operatorId(1L)
                .operatorName("Santosh Gaikwad")
                .farmerId(10L)
                .farmerName("Ramesh Patil")
                .rating(5)
                .comment("Great harvesting service!")
                .createdAt(LocalDateTime.now())
                .build();

        when(reviewService.createOperatorReview(eq(100L), eq(10L), any(OperatorReviewCreateRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/operators/jobs/100/reviews")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.reviewId", is(1)))
                .andExpect(jsonPath("$.data.rating", is(5)));
    }

    @Test
    @DisplayName("Valid Operator JWT Bearer token accessing /api/operators/me/ratings/summary returns 200 OK")
    void validOperatorToken_returnsSummary() throws Exception {
        String token = "valid.operator.token";
        when(jwtService.validateToken(token)).thenReturn(true);
        when(jwtService.extractUserId(token)).thenReturn(1L);
        when(jwtService.extractRole(token)).thenReturn("OPERATOR");
        when(jwtService.extractMobileNumber(token)).thenReturn("9876543220");

        Operator op = new Operator();
        op.setId(1L);
        op.setFullName("Santosh Gaikwad");
        op.setMobileNumber("9876543220");
        op.setActive(true);
        op.setStatus(OperatorStatus.APPROVED);

        when(operatorRepository.findById(1L)).thenReturn(Optional.of(op));

        OperatorRatingSummaryResponse summary = OperatorRatingSummaryResponse.builder()
                .operatorId(1L)
                .averageRating(4.9)
                .totalReviews(5L)
                .fiveStarCount(4L)
                .fourStarCount(1L)
                .threeStarCount(0L)
                .twoStarCount(0L)
                .oneStarCount(0L)
                .build();

        when(reviewService.getOperatorRatingSummary(1L)).thenReturn(summary);

        mockMvc.perform(get("/api/operators/me/ratings/summary")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.averageRating", is(4.9)))
                .andExpect(jsonPath("$.data.totalReviews", is(5)));
    }
}
