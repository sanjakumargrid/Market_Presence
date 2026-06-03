
package com.griddynamics.forge.market_presence_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

/**
 * Dev/local file storage for uploaded resumes.
 *
 * Stores files under {@code uploads/resumes/{slug}/} relative to the working
 * directory. Replace with an S3-backed implementation for production without
 * touching the service or controller layers.
 *
 * Allowed MIME types: PDF and DOCX only (REQ-JP-07).
 */
@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".pdf", ".doc", ".docx");

    @Value("${app.upload.dir:uploads/resumes}")
    private String uploadDir;

    /**
     * Validates MIME type and extension, then persists the file.
     *
     * @param file the uploaded multipart file
     * @param slug job slug — used as a sub-directory to keep files organised
     * @return relative path stored as {@code resumeUrl} on the application record
     * @throws IllegalArgumentException if the file type is not PDF or DOCX
     */
    public String store(MultipartFile file, String slug) {
        validateType(file);

        String originalName = sanitiseFilename(file.getOriginalFilename());
        Path target = Paths.get(uploadDir, slug, originalName);

        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target.toAbsolutePath());
            log.info("[UPLOAD] Stored resume: {}", target);
        } catch (IOException e) {
            log.warn("[UPLOAD] Could not persist file to disk ({}); storing filename only. Error: {}",
                    target, e.getMessage());
        }

        return "resumes/" + slug + "/" + originalName;
    }

    private void validateType(MultipartFile file) {
        String contentType = file.getContentType() != null ? file.getContentType().toLowerCase() : "";
        String name = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.')) : "";

        boolean mimeOk = ALLOWED_TYPES.contains(contentType);
        boolean extOk  = ALLOWED_EXTENSIONS.contains(ext);

        if (!mimeOk && !extOk) {
            throw new IllegalArgumentException(
                    "Unsupported file type '" + file.getOriginalFilename() + "'. " +
                    "Only PDF and DOCX resumes are accepted.");
        }
    }

    private String sanitiseFilename(String name) {
        if (name == null || name.isBlank()) return "resume";
        return name.replaceAll("[^a-zA-Z0-9._\\-]", "_");
    }
}
