package com.placesync.auth.dto;

import com.placesync.user.entity.UserRole;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private long expiresIn;
    private UUID userId;
    private String email;
    private UserRole role;
    private boolean emailVerified;
}
