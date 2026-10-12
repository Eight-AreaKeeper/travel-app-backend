package com.travelapp.user.service;

import com.travelapp.user.dto.UpdateProfileRequestDTO;
import com.travelapp.user.dto.UserProfileDTO;

public interface UserService {

    UserProfileDTO getProfile(Long userId);

    UserProfileDTO updateProfile(Long userId, UpdateProfileRequestDTO request);
}
