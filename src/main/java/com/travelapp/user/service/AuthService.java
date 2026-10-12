package com.travelapp.user.service;

import com.travelapp.user.dto.AuthResponseDTO;
import com.travelapp.user.dto.LoginRequestDTO;
import com.travelapp.user.dto.RefreshResponseDTO;
import com.travelapp.user.dto.RegisterRequestDTO;

public interface AuthService {

    void sendSmsCode(String phone);

    AuthResponseDTO register(RegisterRequestDTO request);

    AuthResponseDTO login(LoginRequestDTO request);

    RefreshResponseDTO refresh(String refreshToken);
}
