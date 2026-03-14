package com.sharemyrecipe.dto;

import com.sharemyrecipe.domain.User;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String handle,
        String displayName,
        String bio,
        String avatarUrl,
        String role,
        Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getHandle(),
                user.getDisplayName(),
                user.getBio(),
                user.getAvatarUrl(),
                user.getRole().name(),
                user.getCreatedAt()
        );
    }
}
