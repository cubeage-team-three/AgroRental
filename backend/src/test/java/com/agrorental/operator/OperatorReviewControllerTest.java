package com.agrorental.operator;

import com.agrorental.common.exception.GlobalExceptionHandler;
import com.agrorental.operator.controller.OperatorReviewController;
import com.agrorental.operator.dto.OperatorRatingSummaryResponse;
import com.agrorental.operator.dto.OperatorReviewCreateRequest;
import com.agrorental.operator.dto.OperatorReviewResponse;
import com.agrorental.operator.service.OperatorReviewService;
import com.agrorental.security.principal.OperatorPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("OperatorReviewController Standalone MockMvc Tests")
class OperatorReviewControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OperatorReviewService reviewService;

    @InjectMocks
    private OperatorReviewController controller;

    @BeforeEach
    void setUp() {
        HandlerMethodArgumentResolver principalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterType().equals(OperatorPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter,
                                          ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest,
                                          WebDataBinderFactory binderFactory) {
                return OperatorPrincipal.builder()
                        .id(1L)
                        .mobileNumber("9876543220")
                        .fullName("Santosh Gaikwad")
                        .role("OPERATOR")
                        .build();
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(principalResolver)
                .build();
    }

    @Test
    @DisplayName("POST /api/operators/jobs/{id}/reviews returns 201 Created on valid review")
    void testCreateReview_success() throws Exception {
        String json = "{\"rating\":5,\"comment\":\"Excellent machine operation and behavior.\"}";

        OperatorReviewResponse response = OperatorReviewResponse.builder()
                .reviewId(50L)
                .assignmentId(100L)
                .bookingId(200L)
                .operatorId(1L)
                .operatorName("Santosh Gaikwad")
                .farmerId(10L)
                .farmerName("Ramesh Patil")
                .rating(5)
                .comment("Excellent machine operation and behavior.")
                .createdAt(LocalDateTime.now())
                .build();

        when(reviewService.createOperatorReview(eq(100L), eq(10L), any(OperatorReviewCreateRequest.class)))
                .thenReturn(response);

        Principal farmerPrincipal = new UsernamePasswordAuthenticationToken(
                "FARMER_10", null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_FARMER"))
        );

        mockMvc.perform(post("/api/operators/jobs/100/reviews")
                        .principal(farmerPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.reviewId", is(50)))
                .andExpect(jsonPath("$.data.rating", is(5)))
                .andExpect(jsonPath("$.data.operatorName", is("Santosh Gaikwad")));
    }

    @Test
    @DisplayName("POST /api/operators/jobs/{id}/reviews with rating out of range returns 400 Bad Request")
    void testCreateReview_invalidRating() throws Exception {
        String json = "{\"rating\":6}";

        Principal farmerPrincipal = new UsernamePasswordAuthenticationToken(
                "FARMER_10", null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_FARMER"))
        );

        mockMvc.perform(post("/api/operators/jobs/100/reviews")
                        .principal(farmerPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("GET /api/operators/{id}/ratings/summary returns 200 OK")
    void testGetRatingSummary_success() throws Exception {
        OperatorRatingSummaryResponse summary = OperatorRatingSummaryResponse.builder()
                .operatorId(1L)
                .averageRating(4.8)
                .totalReviews(10L)
                .fiveStarCount(8L)
                .fourStarCount(2L)
                .threeStarCount(0L)
                .twoStarCount(0L)
                .oneStarCount(0L)
                .build();

        when(reviewService.getOperatorRatingSummary(1L)).thenReturn(summary);

        mockMvc.perform(get("/api/operators/1/ratings/summary")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.averageRating", is(4.8)))
                .andExpect(jsonPath("$.data.totalReviews", is(10)))
                .andExpect(jsonPath("$.data.fiveStarCount", is(8)));
    }

    @Test
    @DisplayName("GET /api/operators/{id}/reviews returns 200 OK with paginated list")
    void testGetReviews_success() throws Exception {
        OperatorReviewResponse item = OperatorReviewResponse.builder()
                .reviewId(1L)
                .assignmentId(100L)
                .bookingId(200L)
                .operatorId(1L)
                .operatorName("Santosh Gaikwad")
                .farmerId(10L)
                .farmerName("Ramesh Patil")
                .rating(5)
                .comment("Very efficient harvesting.")
                .createdAt(LocalDateTime.now())
                .build();

        Page<OperatorReviewResponse> page = new PageImpl<>(List.of(item), PageRequest.of(0, 10), 1);
        when(reviewService.getOperatorReviews(eq(1L), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/operators/1/reviews?page=0&size=10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content[0].rating", is(5)))
                .andExpect(jsonPath("$.data.content[0].comment", is("Very efficient harvesting.")));
    }

    @Test
    @DisplayName("GET /api/operators/me/ratings/summary returns 200 OK for authenticated operator")
    void testGetMyRatingSummary_success() throws Exception {
        OperatorRatingSummaryResponse summary = OperatorRatingSummaryResponse.builder()
                .operatorId(1L)
                .averageRating(5.0)
                .totalReviews(1L)
                .fiveStarCount(1L)
                .fourStarCount(0L)
                .threeStarCount(0L)
                .twoStarCount(0L)
                .oneStarCount(0L)
                .build();

        when(reviewService.getOperatorRatingSummary(1L)).thenReturn(summary);

        mockMvc.perform(get("/api/operators/me/ratings/summary")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.averageRating", is(5.0)));
    }
}
