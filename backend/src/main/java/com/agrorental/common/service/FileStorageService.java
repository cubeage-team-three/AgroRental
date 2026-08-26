package com.agrorental.common.service;

import com.agrorental.common.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Service for securely storing uploaded equipment images to the local filesystem
 * and generating accessible web paths.
 */
@Slf4j
@Service
public class FileStorageService {

    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp"
    );

    private final Path uploadPath;

    public FileStorageService(@Value("${file.upload-dir:uploads}") String uploadDir) {
        this.uploadPath = Paths.get(uploadDir, "equipment").toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadPath);
            log.info("Equipment upload directory initialized at: {}", this.uploadPath);
        } catch (IOException e) {
            log.error("Could not create equipment upload directory", e);
            throw new RuntimeException("Failed to initialize file storage directory.", e);
        }
    }

    /**
     * Stores an uploaded equipment image and returns its relative URL path.
     *
     * @param file Uploaded MultipartFile
     * @return Accessible relative image URL string (e.g. "/uploads/equipment/uuid.png")
     */
    public String storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Failed to store file: Uploaded image file is empty.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException("Unsupported image format. Allowed formats: JPEG, PNG, WebP.");
        }

        String originalFilename = StringUtils.cleanPath(Objects.requireNonNullElse(file.getOriginalFilename(), "file.png"));
        String extension = getFileExtension(originalFilename, contentType);

        String generatedFilename = UUID.randomUUID().toString() + extension;

        try {
            Path targetLocation = this.uploadPath.resolve(generatedFilename).normalize();

            // Safety check against path traversal
            if (!targetLocation.getParent().equals(this.uploadPath)) {
                throw new BadRequestException("Cannot store file outside current directory.");
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }

            log.info("Successfully stored equipment image file: {}", generatedFilename);
            return "/uploads/equipment/" + generatedFilename;
        } catch (IOException ex) {
            log.error("Failed to store file {}", generatedFilename, ex);
            throw new BadRequestException("Could not store file. Please try again!");
        }
    }

    private String getFileExtension(String filename, String contentType) {
        if (filename.contains(".")) {
            String ext = filename.substring(filename.lastIndexOf(".")).toLowerCase();
            if (List.of(".jpg", ".jpeg", ".png", ".webp").contains(ext)) {
                return ext;
            }
        }

        return switch (contentType.toLowerCase()) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }

    public Path getUploadPath() {
        return uploadPath;
    }
}
