package com.travelapp.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travelapp.common.BusinessException;
import com.travelapp.common.ErrorCode;
import com.travelapp.security.JwtService;
import com.travelapp.user.dto.AuthResponseDTO;
import com.travelapp.user.dto.LoginRequestDTO;
import com.travelapp.user.dto.RefreshResponseDTO;
import com.travelapp.user.dto.RegisterRequestDTO;
import com.travelapp.user.entity.User;
import com.travelapp.user.mapper.UserMapper;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SmsCodeService smsCodeService;

    @Override
    public void sendSmsCode(String phone) {
        smsCodeService.sendCode(phone);
    }

    @Override
    public AuthResponseDTO register(RegisterRequestDTO request) {
        boolean phoneTaken = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getPhone, request.getPhone())) > 0;
        if (phoneTaken) {
            throw new BusinessException(ErrorCode.CONFLICT, "This phone number is already registered");
        }
        if (!smsCodeService.verifyCode(request.getPhone(), request.getCode())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "Verification code is incorrect or has expired");
        }

        User user = new User();
        user.setPhone(request.getPhone());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setNickname(hasText(request.getNickname()) ? request.getNickname() : defaultNickname(request.getPhone()));
        user.setStatus(0);
        userMapper.insert(user);

        return issueTokens(user.getId());
    }

    @Override
    public AuthResponseDTO login(LoginRequestDTO request) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getPhone, request.getPhone()));
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Incorrect phone number or password");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Incorrect phone number or password");
        }
        if (Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Account has been banned");
        }

        return issueTokens(user.getId());
    }

    @Override
    public RefreshResponseDTO refresh(String refreshToken) {
        Long userId;
        try {
            userId = jwtService.parseRefreshTokenUserId(refreshToken);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "refreshToken is invalid or has expired");
        }

        if (userMapper.selectById(userId) == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "User does not exist");
        }

        return RefreshResponseDTO.builder()
                .accessToken(jwtService.generateAccessToken(userId))
                .expiresIn(jwtService.getAccessTokenExpireSeconds())
                .build();
    }

    private AuthResponseDTO issueTokens(Long userId) {
        return AuthResponseDTO.builder()
                .userId(userId)
                .accessToken(jwtService.generateAccessToken(userId))
                .refreshToken(jwtService.generateRefreshToken(userId))
                .expiresIn(jwtService.getAccessTokenExpireSeconds())
                .build();
    }

    private boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    private String defaultNickname(String phone) {
        String suffix = phone.length() >= 4 ? phone.substring(phone.length() - 4) : phone;
        return "Traveler" + suffix;
    }
}
