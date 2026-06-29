package com.placesync.common.admin.controller;

import com.placesync.common.admin.dto.UpdateUserStatusRequest;
import com.placesync.common.admin.dto.UserSummaryResponse;
import com.placesync.common.admin.service.AdminUserService;
import com.placesync.common.util.PagedResponse;
import com.placesync.user.entity.UserRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin — Users", description = "User management — admin only")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    @Operation(summary = "Search users with optional filters")
    public ResponseEntity<PagedResponse<UserSummaryResponse>> searchUsers(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) Boolean isActive,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(adminUserService.searchUsers(email, role, isActive, pageable));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get user detail by ID")
    public ResponseEntity<UserSummaryResponse> getUserById(@PathVariable UUID userId) {
        return ResponseEntity.ok(adminUserService.getUserById(userId));
    }

    @PatchMapping("/{userId}/status")
    @Operation(summary = "Activate or deactivate a user")
    public ResponseEntity<UserSummaryResponse> updateUserStatus(
            @PathVariable UUID userId,
            @RequestBody @Valid UpdateUserStatusRequest req) {
        return ResponseEntity.ok(adminUserService.updateUserStatus(userId, req));
    }
}
