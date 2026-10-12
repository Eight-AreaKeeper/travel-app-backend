package com.travelapp.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Corresponds to API Specification 2.1 POST /auth/register
 */
@Getter
@Setter
public class RegisterRequestDTO {

    @NotBlank(message = "Phone number cannot be empty")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "Invalid phone number format")
    private String phone;

    @NotBlank(message = "Verification code cannot be empty")
    @Pattern(regexp = "^\\d{6}$", message = "Invalid verification code format")
    private String code;

    @NotBlank(message = "Password cannot be empty")
    @Size(min = 6, max = 20, message = "Password length must be between 6 and 20 characters")
    private String password;

    /** If not provided, the system generates a default nickname */
    private String nickname;
}
