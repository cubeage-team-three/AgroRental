package com.agrorental.common.controller;

import com.agrorental.common.dto.ApiResponse;
import com.agrorental.common.dto.ImageUploadResponse;
import com.agrorental.common.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST Controller providing secure image upload APIs for AgroRental equipment listings.
 */
@Slf4j
@RestController
@RequestMapping("/api/upload")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileStorageService fileStorageService;

    /**
     * Uploads an equipment image file (JPEG, PNG, WebP) and returns its relative public URL.
     * Restricted to authenticated PARTNER users.
     *
     * @param file Image file payload
     * @return ResponseEntity containing HTTP 201 Created and ImageUploadResponse payload
     */
    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('PARTNER')")
    public ResponseEntity<ApiResponse<ImageUploadResponse>> uploadImage(
            @RequestParam("file") MultipartFile file) {

        log.info("Received image upload request: name={}, size={}, contentType={}",
                file.getOriginalFilename(), file.getSize(), file.getContentType());

        String fileUrl = fileStorageService.storeFile(file);

        ImageUploadResponse response = ImageUploadResponse.builder()
                .url(fileUrl)
                .build();

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Image uploaded successfully", response));
    }
}
