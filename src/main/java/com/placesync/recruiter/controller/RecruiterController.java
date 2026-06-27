package com.placesync.recruiter.controller;

import com.placesync.common.security.UserPrincipal;
import com.placesync.common.util.PagedResponse;
import com.placesync.recruiter.dto.RecruiterProfileResponse;
import com.placesync.recruiter.dto.RecruiterVerificationRequest;
import com.placesync.recruiter.dto.UpdateRecruiterProfileRequest;
import com.placesync.recruiter.service.RecruiterService;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Recruiter", description = "Recruiter profile and verification management")
public class RecruiterController {

    private final RecruiterService recruiterService;

    @GetMapping("/api/v1/recruiters/profile")
    @PreAuthorize("hasAuthority('ROLE_RECRUITER')")
    @Operation(summary = "Get own recruiter profile")
    public ResponseEntity<RecruiterProfileResponse> getProfile(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(recruiterService.getMyProfile(principal.getId()));
    }

    @PutMapping("/api/v1/recruiters/profile")
    @PreAuthorize("hasAuthority('ROLE_RECRUITER')")
    @Operation(summary = "Update recruiter profile")
    public ResponseEntity<RecruiterProfileResponse> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateRecruiterProfileRequest req) {
        return ResponseEntity.ok(recruiterService.updateProfile(principal.getId(), req));
    }

    // ── Admin endpoints ───────────────────────────────────────────────────────

    @GetMapping("/api/v1/admin/recruiters/pending")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "List recruiters pending verification (admin only)")
    public ResponseEntity<PagedResponse<RecruiterProfileResponse>> getPendingVerifications(
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(recruiterService.getPendingVerifications(pageable));
    }

    @PatchMapping("/api/v1/admin/recruiters/{recruiterId}/verify")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Approve or reject a recruiter verification (admin only)")
    public ResponseEntity<RecruiterProfileResponse> processVerification(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID recruiterId,
            @Valid @RequestBody RecruiterVerificationRequest req) {
        return ResponseEntity.ok(recruiterService.processVerification(principal.getId(), recruiterId, req));
    }
}
