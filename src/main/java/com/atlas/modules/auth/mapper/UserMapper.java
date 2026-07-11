package com.atlas.modules.auth.mapper;

import com.atlas.modules.auth.dto.UserResponse;
import com.atlas.modules.auth.entity.User;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getAvatarUrl(),
                user.isEmailVerified(),
                user.getStatus().name(),
                user.getCreatedAt()
        );
    }
}
