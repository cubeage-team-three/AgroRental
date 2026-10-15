package com.agrorental.operator;

import com.agrorental.common.exception.GlobalExceptionHandler;
import com.agrorental.operator.controller.OperatorDocumentController;
import com.agrorental.operator.dto.OperatorDocumentCreateRequest;
import com.agrorental.operator.dto.OperatorDocumentResponse;
import com.agrorental.operator.enums.DocumentStatus;
import com.agrorental.operator.enums.DocumentType;
import com.agrorental.operator.service.OperatorDocumentService;
import com.agrorental.security.principal.OperatorPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("OperatorDocumentController MockMvc Unit Tests")
class OperatorDocumentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OperatorDocumentService documentService;

    @InjectMocks
    private OperatorDocumentController documentController;

    private OperatorDocumentResponse testDocResponse;

    /** Authenticated, approved operator #1 (the document owner). */
    private static UsernamePasswordAuthenticationToken owningOperator() {
        OperatorPrincipal principal = OperatorPrincipal.builder().id(1L).mobileNumber("9876543210")
                .fullName("Owner Operator").role("OPERATOR").build();
        return new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_OPERATOR")));
    }

    private static UsernamePasswordAuthenticationToken administrator() {
        return new UsernamePasswordAuthenticationToken("ADMIN_1", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(documentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        testDocResponse = OperatorDocumentResponse.builder()
                .id(10L)
                .documentType(DocumentType.AADHAAR)
                .maskedDocumentNumber("XXXX-XXXX-9012")
                .fileName("aadhaar.pdf")
                .fileUrl("https://storage.agrorental.com/aadhaar.pdf")
                .fileSize(102400L)
                .mimeType("application/pdf")
                .verificationStatus(DocumentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("POST /api/operators/{operatorId}/documents - Should return 201 Created on valid upload")
    void shouldUploadDocument() throws Exception {
        when(documentService.addDocument(eq(1L), any(OperatorDocumentCreateRequest.class), eq(false)))
                .thenReturn(testDocResponse);

        String jsonPayload = """
                {
                    "documentType": "AADHAAR",
                    "documentNumber": "123456789012",
                    "fileName": "aadhaar.pdf",
                    "fileUrl": "https://storage.agrorental.com/aadhaar.pdf",
                    "fileSize": 102400,
                    "mimeType": "application/pdf"
                }
                """;

        mockMvc.perform(post("/api/operators/1/documents").principal(owningOperator())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.documentType").value("AADHAAR"))
                .andExpect(jsonPath("$.data.maskedDocumentNumber").value("XXXX-XXXX-9012"));
    }

    @Test
    @DisplayName("GET /api/operators/{operatorId}/documents - Should return 200 OK with list")
    void shouldGetDocuments() throws Exception {
        when(documentService.getOperatorDocuments(1L))
                .thenReturn(Collections.singletonList(testDocResponse));

        mockMvc.perform(get("/api/operators/1/documents").principal(administrator()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(10))
                .andExpect(jsonPath("$.data[0].fileName").value("aadhaar.pdf"));
    }
}
