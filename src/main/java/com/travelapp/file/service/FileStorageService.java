package com.travelapp.file.service;

import com.travelapp.common.BusinessException;
import com.travelapp.common.ErrorCode;
import com.travelapp.file.dto.UploadResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

/**
 * Corresponds to section 2.6 POST /files/upload of the API docs.
 * Local disk storage implementation — stores files under the directory specified by
 * app.upload.dir, split into subdirectories by scene, and exposed externally via the static
 * resource mapping registered in WebMvcConfig. In production, consider switching to object
 * storage (e.g. S3/OSS); only this class's implementation needs to change, the Controller/DTO
 * stay untouched.
 */
@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;
    private static final String DEFAULT_SCENE = "note_content";

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Value("${app.upload.base-url}")
    private String baseUrl;

    public UploadResponseDTO store(MultipartFile file, String scene) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "File cannot be empty");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "File size cannot exceed 5MB");
        }
        String extension = extensionOf(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "Only jpg/png/webp image formats are supported");
        }
        String safeScene = sanitizeScene(scene);
        String filename = UUID.randomUUID() + "." + extension;

        try {
            Path dir = Paths.get(uploadDir, safeScene);
            Files.createDirectories(dir);
            file.transferTo(dir.resolve(filename));
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Failed to save file");
        }

        String url = baseUrl + "/uploads/" + safeScene + "/" + filename;
        return UploadResponseDTO.builder().url(url).build();
    }

    private String sanitizeScene(String scene) {
        if (scene == null || scene.isBlank()) return DEFAULT_SCENE;
        String cleaned = scene.replaceAll("[^a-zA-Z0-9_]", "");
        return cleaned.isBlank() ? DEFAULT_SCENE : cleaned;
    }

    private String extensionOf(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
