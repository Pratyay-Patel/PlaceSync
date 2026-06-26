package com.placesync.user.controller;

import com.placesync.common.security.UserPrincipal;
import com.placesync.user.dto.CreateResumeRequest;
import com.placesync.user.dto.ResumeResponse;
import com.placesync.user.service.ResumeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/students/resumes")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Resume", description = "Student resume metadata management")
public class ResumeController {

    private final ResumeService resumeService;

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    @Operation(summary = "List own resumes")
    public ResponseEntity<List<ResumeResponse>> getMyResumes(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(resumeService.getMyResumes(principal.getId()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    @Operation(summary = "Register resume metadata (S3 upload available in Phase 5)")
    public ResponseEntity<ResumeResponse> createResume(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateResumeRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(resumeService.createResume(principal.getId(), req));
    }

    @PatchMapping("/{resumeId}/default")
    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    @Operation(summary = "Set resume as default")
    public ResponseEntity<ResumeResponse> setDefault(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID resumeId) {
        return ResponseEntity.ok(resumeService.setDefault(principal.getId(), resumeId));
    }

    @DeleteMapping("/{resumeId}")
    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    @Operation(summary = "Soft-delete a resume")
    public ResponseEntity<Void> deleteResume(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID resumeId) {
        resumeService.softDelete(principal.getId(), resumeId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{resumeId}/url")
    @PreAuthorize("hasAuthority('ROLE_STUDENT') or hasAuthority('ROLE_RECRUITER')")
    @Operation(summary = "Get pre-signed download URL (available in Phase 5)")
    public ResponseEntity<Void> getDownloadUrl(@PathVariable UUID resumeId) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
