package com.placesync.application.controller;

import com.placesync.application.dto.ApplyRequest;
import com.placesync.application.dto.ApplicationResponse;
import com.placesync.application.dto.UpdateApplicationStatusRequest;
import com.placesync.application.service.ApplicationService;
import com.placesync.common.security.UserPrincipal;
import com.placesync.common.util.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Application", description = "Job application management")
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping("/applications")
    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    @Operation(summary = "Apply to a job")
    public ResponseEntity<ApplicationResponse> apply(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ApplyRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(applicationService.apply(principal.getId(), req));
    }

    @GetMapping("/students/applications")
    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    @Operation(summary = "List own applications (paginated)")
    public ResponseEntity<PagedResponse<ApplicationResponse>> getMyApplications(
            @AuthenticationPrincipal UserPrincipal principal,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(applicationService.getMyApplications(principal.getId(), pageable));
    }

    @GetMapping("/students/applications/{applicationId}")
    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    @Operation(summary = "Get own application detail")
    public ResponseEntity<ApplicationResponse> getMyApplication(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID applicationId) {
        return ResponseEntity.ok(applicationService.getMyApplication(principal.getId(), applicationId));
    }

    @GetMapping("/recruiters/jobs/{jobId}/applications")
    @PreAuthorize("hasAuthority('ROLE_RECRUITER')")
    @Operation(summary = "List applicants for a job")
    public ResponseEntity<PagedResponse<ApplicationResponse>> getJobApplications(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID jobId,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(applicationService.getJobApplications(principal.getId(), jobId, pageable));
    }

    @PatchMapping("/recruiters/applications/{applicationId}/status")
    @PreAuthorize("hasAuthority('ROLE_RECRUITER')")
    @Operation(summary = "Update application status")
    public ResponseEntity<ApplicationResponse> updateStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID applicationId,
            @Valid @RequestBody UpdateApplicationStatusRequest req) {
        return ResponseEntity.ok(applicationService.updateStatus(principal.getId(), applicationId, req));
    }
}
