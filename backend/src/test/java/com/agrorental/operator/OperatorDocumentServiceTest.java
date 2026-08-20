package com.agrorental.operator;

import com.agrorental.common.exception.ResourceNotFoundException;
import com.agrorental.operator.dto.OperatorDocumentCreateRequest;
import com.agrorental.operator.dto.OperatorDocumentResponse;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorDocument;
import com.agrorental.operator.enums.DocumentStatus;
import com.agrorental.operator.enums.DocumentType;
import com.agrorental.operator.mapper.OperatorDocumentMapper;
import com.agrorental.operator.repository.OperatorDocumentRepository;
import com.agrorental.operator.repository.OperatorRepository;
import com.agrorental.operator.service.OperatorDocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OperatorDocumentService Unit Tests")
class OperatorDocumentServiceTest {

    @Mock
    private OperatorDocumentRepository documentRepository;

    @Mock
    private OperatorRepository operatorRepository;

    @Spy
    private OperatorDocumentMapper documentMapper = new OperatorDocumentMapper();

    @InjectMocks
    private OperatorDocumentService documentService;

    private Operator testOperator;

    @BeforeEach
    void setUp() {
        testOperator = Operator.builder()
                .fullName("Suresh Shinde")
                .mobileNumber("9876543210")
                .build();
        testOperator.setId(1L);
    }

    @Test
    @DisplayName("addDocument() - Should successfully persist document metadata and return response")
    void shouldAddDocumentSuccessfully() {
        OperatorDocumentCreateRequest request = OperatorDocumentCreateRequest.builder()
                .documentType(DocumentType.AADHAAR)
                .documentNumber("123456789012")
                .fileName("aadhaar_card.pdf")
                .fileUrl("https://storage.agrorental.com/aadhaar_card.pdf")
                .fileSize(204800L)
                .mimeType("application/pdf")
                .build();

        when(operatorRepository.findById(1L)).thenReturn(Optional.of(testOperator));
        when(documentRepository.findByOperatorIdAndDocumentType(1L, DocumentType.AADHAAR))
                .thenReturn(Optional.empty());

        when(documentRepository.save(any(OperatorDocument.class))).thenAnswer(i -> {
            OperatorDocument doc = i.getArgument(0);
            doc.setId(10L);
            doc.setCreatedAt(LocalDateTime.now());
            return doc;
        });

        OperatorDocumentResponse response = documentService.addDocument(1L, request);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals(DocumentType.AADHAAR, response.getDocumentType());
        assertEquals("XXXX-XXXX-9012", response.getMaskedDocumentNumber());
        assertEquals("aadhaar_card.pdf", response.getFileName());
        assertEquals(DocumentStatus.PENDING, response.getVerificationStatus());

        verify(documentRepository).save(any(OperatorDocument.class));
    }

    @Test
    @DisplayName("getOperatorDocuments() - Should return list of operator documents")
    void shouldGetDocumentsList() {
        OperatorDocument doc = OperatorDocument.builder()
                .documentType(DocumentType.DRIVING_LICENSE)
                .fileName("dl.png")
                .fileUrl("https://storage.agrorental.com/dl.png")
                .verificationStatus(DocumentStatus.PENDING)
                .build();
        doc.setId(20L);
        doc.setCreatedAt(LocalDateTime.now());

        when(operatorRepository.existsById(1L)).thenReturn(true);
        when(documentRepository.findByOperatorId(1L)).thenReturn(Collections.singletonList(doc));

        List<OperatorDocumentResponse> list = documentService.getOperatorDocuments(1L);

        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals(DocumentType.DRIVING_LICENSE, list.get(0).getDocumentType());
    }

    @Test
    @DisplayName("addDocument() - Should throw ResourceNotFoundException when operator does not exist")
    void shouldThrowNotFoundWhenOperatorMissing() {
        OperatorDocumentCreateRequest request = OperatorDocumentCreateRequest.builder()
                .documentType(DocumentType.AADHAAR)
                .fileName("aadhaar.pdf")
                .fileUrl("url")
                .build();

        when(operatorRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> documentService.addDocument(999L, request));
    }
}
