package com.travelapp.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Corresponds to the response structure of API Specification 2.1/2.2
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDTO {
    private Long userId;
    private String accessToken;
    private String refreshToken;
    private long expiresIn;
}
