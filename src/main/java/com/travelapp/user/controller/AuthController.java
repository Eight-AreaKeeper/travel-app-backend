package com.travelapp.user.controller;

import com.travelapp.common.ApiResponse;
import com.travelapp.user.dto.AuthResponseDTO;
import com.travelapp.user.dto.LoginRequestDTO;
import com.travelapp.user.dto.RefreshRequestDTO;
import com.travelapp.user.dto.RefreshResponseDTO;
import com.travelapp.user.dto.RegisterRequestDTO;
import com.travelapp.user.dto.SmsCodeRequestDTO;
import com.travelapp.user.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Corresponds to the register/login/refresh-token section of the User Module (Section 2) in the
 * "API Specification", as well as the SMS verification code endpoint mentioned in "pending item 3".
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth Module")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/sms-code")
    public ApiResponse<Void> sendSmsCode(@Valid @RequestBody SmsCodeRequestDTO request) {
        authService.sendSmsCode(request.getPhone());
        return ApiResponse.success();
    }

    @PostMapping("/register")
    public ApiResponse<AuthResponseDTO> register(@Valid @RequestBody RegisterRequestDTO request) {
        return ApiResponse.success(authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<RefreshResponseDTO> refresh(@Valid @RequestBody RefreshRequestDTO request) {
        return ApiResponse.success(authService.refresh(request.getRefreshToken()));
    }
}
