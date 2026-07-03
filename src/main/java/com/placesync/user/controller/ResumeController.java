package com.placesync.user.controller;

import com.placesync.common.security.UserPrincipal;
import com.placesync.user.dto.ResumeDownloadUrlResponse;
import com.placesync.user.dto.ResumeResponse;
import com.placesync.user.service.ResumeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/students/resumes")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Resume", description = "Student resume management")
public class ResumeController {

    private final ResumeService resumeService;

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    @Operation(summary = "List own resumes")
    public ResponseEntity<List<ResumeResponse>> getMyResumes(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(resumeService.getMyResumes(principal.getId()));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    @Operation(summary = "Upload a resume PDF to S3")
    public ResponseEntity<ResumeResponse> uploadResume(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestPart("file") MultipartFile file,
            @RequestParam @NotBlank String label,
            @RequestParam(defaultValue = "false") boolean isDefault) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(resumeService.uploadResume(principal.getId(), file, label, isDefault));
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
    @Operation(summary = "Get pre-signed download URL (15-min TTL)")
    public ResponseEntity<ResumeDownloadUrlResponse> getDownloadUrl(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID resumeId) {
        return ResponseEntity.ok(
                resumeService.getDownloadUrl(principal.getId(), resumeId, principal.getRole()));
    }
}
