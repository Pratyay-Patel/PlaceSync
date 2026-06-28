package com.placesync.job.controller;

import com.placesync.common.security.UserPrincipal;
import com.placesync.common.util.PagedResponse;
import com.placesync.job.dto.*;
import com.placesync.job.service.JobService;
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
@Tag(name = "Job", description = "Job posting management")
public class JobController {

    private final JobService jobService;

    @GetMapping("/jobs")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List open jobs with optional filters (paginated, cached per filter combination)")
    public ResponseEntity<PagedResponse<JobSummaryResponse>> getOpenJobs(
            @ParameterObject JobFilterRequest filter,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(jobService.getOpenJobs(filter, pageable));
    }

    @GetMapping("/jobs/{jobId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get job detail — cached")
    public ResponseEntity<JobResponse> getJob(@PathVariable UUID jobId) {
        return ResponseEntity.ok(jobService.getJob(jobId));
    }

    @PostMapping("/jobs")
    @PreAuthorize("hasAuthority('ROLE_RECRUITER')")
    @Operation(summary = "Create a job posting (starts as PENDING_APPROVAL)")
    public ResponseEntity<JobResponse> createJob(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateJobRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(jobService.createJob(principal.getId(), req));
    }

    @PutMapping("/jobs/{jobId}")
    @PreAuthorize("hasAuthority('ROLE_RECRUITER')")
    @Operation(summary = "Update a job (DRAFT or PENDING_APPROVAL only)")
    public ResponseEntity<JobResponse> updateJob(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID jobId,
            @Valid @RequestBody UpdateJobRequest req) {
        return ResponseEntity.ok(jobService.updateJob(principal.getId(), jobId, req));
    }

    @DeleteMapping("/jobs/{jobId}")
    @PreAuthorize("hasAuthority('ROLE_RECRUITER')")
    @Operation(summary = "Soft-delete a job posting")
    public ResponseEntity<Void> deleteJob(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID jobId) {
        jobService.softDeleteJob(principal.getId(), jobId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/jobs/{jobId}/close")
    @PreAuthorize("hasAuthority('ROLE_RECRUITER')")
    @Operation(summary = "Close a job (set status = CLOSED)")
    public ResponseEntity<JobResponse> closeJob(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID jobId) {
        return ResponseEntity.ok(jobService.closeJob(principal.getId(), jobId));
    }

    @GetMapping("/recruiters/jobs")
    @PreAuthorize("hasAuthority('ROLE_RECRUITER')")
    @Operation(summary = "List own job postings")
    public ResponseEntity<PagedResponse<JobSummaryResponse>> getRecruiterJobs(
            @AuthenticationPrincipal UserPrincipal principal,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(jobService.getRecruiterJobs(principal.getId(), pageable));
    }

    @GetMapping("/admin/jobs/pending")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "List jobs pending approval (admin only)")
    public ResponseEntity<PagedResponse<JobSummaryResponse>> getPendingJobs(
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(jobService.getPendingJobs(pageable));
    }

    @PatchMapping("/admin/jobs/{jobId}/approve")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Approve or reject a job (admin only)")
    public ResponseEntity<JobResponse> processApproval(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID jobId,
            @Valid @RequestBody JobApprovalRequest req) {
        return ResponseEntity.ok(jobService.processApproval(principal.getId(), jobId, req));
    }
}
