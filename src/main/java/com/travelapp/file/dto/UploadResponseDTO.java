package com.travelapp.file.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Response structure for section 2.6 POST /files/upload of the API docs
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadResponseDTO {
    private String url;
}
