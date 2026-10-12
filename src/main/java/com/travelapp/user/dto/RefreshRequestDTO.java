package com.travelapp.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Corresponds to API Specification 2.3 POST /auth/refresh
 */
@Getter
@Setter
public class RefreshRequestDTO {

    @NotBlank(message = "refreshToken cannot be empty")
    private String refreshToken;
}
