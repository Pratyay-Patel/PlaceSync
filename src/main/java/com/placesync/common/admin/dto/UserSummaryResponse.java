package com.placesync.common.admin.dto;

import com.placesync.user.entity.UserRole;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
public class UserSummaryResponse {
    private UUID id;
    private String email;
    private UserRole role;
    private Boolean isActive;
    private Boolean isEmailVerified;
    private OffsetDateTime createdAt;
}
