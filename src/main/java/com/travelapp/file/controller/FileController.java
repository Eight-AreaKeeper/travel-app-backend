package com.travelapp.file.controller;

import com.travelapp.common.ApiResponse;
import com.travelapp.file.dto.UploadResponseDTO;
import com.travelapp.file.service.FileStorageService;
import com.travelapp.security.CurrentUserProvider;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Corresponds to section 2.6 POST /files/upload of the API docs */
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Tag(name = "File Module")
public class FileController {

    private final FileStorageService fileStorageService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UploadResponseDTO> upload(@RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String scene) {
        currentUserProvider.requireUserId();
        return ApiResponse.success(fileStorageService.store(file, scene));
    }
}
