package com.travelapp.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Corresponds to API Specification 2.5 PATCH /users/me -- all fields are optional, only the fields provided are updated
 */
@Getter
@Setter
public class UpdateProfileRequestDTO {

    @Size(max = 32, message = "Nickname length cannot exceed 32 characters")
    private String nickname;

    private String avatarUrl;

    /** 0 unknown / 1 male / 2 female */
    @Min(value = 0, message = "gender must be in range 0-2")
    @Max(value = 2, message = "gender must be in range 0-2")
    private Integer gender;

    private LocalDate birthday;
}
