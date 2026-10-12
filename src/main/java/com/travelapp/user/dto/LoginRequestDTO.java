package com.travelapp.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Corresponds to API Specification 2.2 POST /auth/login
 */
@Getter
@Setter
public class LoginRequestDTO {

    @NotBlank(message = "Phone number cannot be empty")
    private String phone;

    @NotBlank(message = "Password cannot be empty")
    private String password;
}
