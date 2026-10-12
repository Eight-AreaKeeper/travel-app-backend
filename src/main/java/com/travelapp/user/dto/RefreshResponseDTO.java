package com.travelapp.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Corresponds to the response structure of API Specification 2.3
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshResponseDTO {
    private String accessToken;
    private long expiresIn;
}
