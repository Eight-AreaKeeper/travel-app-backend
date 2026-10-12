package com.travelapp.user.service;

import com.travelapp.common.BusinessException;
import com.travelapp.common.ErrorCode;
import com.travelapp.user.dto.UpdateProfileRequestDTO;
import com.travelapp.user.dto.UserProfileDTO;
import com.travelapp.user.entity.User;
import com.travelapp.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    @Override
    public UserProfileDTO getProfile(Long userId) {
        return toProfile(requireUser(userId));
    }

    @Override
    @Transactional
    public UserProfileDTO updateProfile(Long userId, UpdateProfileRequestDTO request) {
        User user = requireUser(userId);
        if (request.getNickname() != null) user.setNickname(request.getNickname());
        if (request.getAvatarUrl() != null) user.setAvatarUrl(request.getAvatarUrl());
        if (request.getGender() != null) user.setGender(request.getGender());
        if (request.getBirthday() != null) user.setBirthday(request.getBirthday());
        userMapper.updateById(user);
        return toProfile(user);
    }

    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "User does not exist");
        }
        return user;
    }

    private UserProfileDTO toProfile(User user) {
        return UserProfileDTO.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .phone(user.getPhone())
                .gender(user.getGender())
                .birthday(user.getBirthday())
                .build();
    }
}
