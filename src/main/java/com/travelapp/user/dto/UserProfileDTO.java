package com.travelapp.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Corresponds to the response structure of API Specification 2.4 GET /users/me
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDTO {
    private Long id;
    private String nickname;
    private String avatarUrl;
    private String phone;
    private Integer gender;
    private LocalDate birthday;
}
