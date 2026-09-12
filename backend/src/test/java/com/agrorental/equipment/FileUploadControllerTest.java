package com.agrorental.equipment;

import com.agrorental.common.controller.FileUploadController;
import com.agrorental.common.exception.GlobalExceptionHandler;
import com.agrorental.common.service.FileStorageService;
import com.agrorental.security.principal.PartnerPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.file.Path;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileUploadController Standalone MockMvc Unit Tests")
class FileUploadControllerTest {

    private MockMvc mockMvc;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private FileUploadController fileUploadController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(fileUploadController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAsPartner() {
        PartnerPrincipal principal = PartnerPrincipal.builder().id(1L).role("PARTNER").build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                principal, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_PARTNER"))));
    }

    @Test
    @DisplayName("POST /api/upload/image - Valid JPEG should return 201 Created")
    void uploadValidJpeg_shouldReturn201() throws Exception {
        authenticateAsPartner();
        MockMultipartFile file = new MockMultipartFile(
                "file", "tractor.jpg", "image/jpeg", "fake-jpeg-binary-data".getBytes()
        );

        when(fileStorageService.storeFile(any())).thenReturn("/uploads/equipment/abc-123.jpg");

        mockMvc.perform(multipart("/api/upload/image").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.url").value("/uploads/equipment/abc-123.jpg"));
    }

    @Test
    @DisplayName("POST /api/upload/image - Valid PNG should return 201 Created")
    void uploadValidPng_shouldReturn201() throws Exception {
        authenticateAsPartner();
        MockMultipartFile file = new MockMultipartFile(
                "file", "harvester.png", "image/png", "fake-png-binary-data".getBytes()
        );

        when(fileStorageService.storeFile(any())).thenReturn("/uploads/equipment/def-456.png");

        mockMvc.perform(multipart("/api/upload/image").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.url").value("/uploads/equipment/def-456.png"));
    }

    @Test
    @DisplayName("POST /api/upload/image - Valid WebP should return 201 Created")
    void uploadValidWebp_shouldReturn201() throws Exception {
        authenticateAsPartner();
        MockMultipartFile file = new MockMultipartFile(
                "file", "rotavator.webp", "image/webp", "fake-webp-binary-data".getBytes()
        );

        when(fileStorageService.storeFile(any())).thenReturn("/uploads/equipment/ghi-789.webp");

        mockMvc.perform(multipart("/api/upload/image").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.url").value("/uploads/equipment/ghi-789.webp"));
    }

    @Test
    @DisplayName("POST /api/upload/image - Empty file should return 400 Bad Request")
    void uploadEmptyFile_shouldReturn400() throws Exception {
        authenticateAsPartner();
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.png", "image/png", new byte[0]
        );

        when(fileStorageService.storeFile(any()))
                .thenThrow(new com.agrorental.common.exception.BadRequestException("Failed to store file: Uploaded image file is empty."));

        mockMvc.perform(multipart("/api/upload/image").file(emptyFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Failed to store file: Uploaded image file is empty."));
    }

    @Test
    @DisplayName("POST /api/upload/image - Unsupported file type should return 400 Bad Request")
    void uploadUnsupportedType_shouldReturn400() throws Exception {
        authenticateAsPartner();
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file", "document.pdf", "application/pdf", "fake-pdf".getBytes()
        );

        when(fileStorageService.storeFile(any()))
                .thenThrow(new com.agrorental.common.exception.BadRequestException("Unsupported image format. Allowed formats: JPEG, PNG, WebP."));

        mockMvc.perform(multipart("/api/upload/image").file(pdfFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Unsupported image format. Allowed formats: JPEG, PNG, WebP."));
    }
}
