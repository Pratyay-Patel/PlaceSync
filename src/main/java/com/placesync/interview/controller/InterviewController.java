package com.placesync.interview.controller;

import com.placesync.common.security.UserPrincipal;
import com.placesync.interview.dto.*;
import com.placesync.interview.service.InterviewService;
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
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Interview", description = "Interview scheduling management")
public class InterviewController {

    private final InterviewService interviewService;

    @GetMapping("/students/interviews")
    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    @Operation(summary = "List own upcoming interviews")
    public ResponseEntity<List<InterviewResponse>> getMyInterviews(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(interviewService.getMyInterviews(principal.getId()));
    }

    @GetMapping("/recruiters/applications/{applicationId}/interviews")
    @PreAuthorize("hasAuthority('ROLE_RECRUITER')")
    @Operation(summary = "List interviews for an application")
    public ResponseEntity<List<InterviewResponse>> getApplicationInterviews(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID applicationId) {
        return ResponseEntity.ok(interviewService.getApplicationInterviews(principal.getId(), applicationId));
    }

    @PostMapping("/recruiters/applications/{applicationId}/interviews")
    @PreAuthorize("hasAuthority('ROLE_RECRUITER')")
    @Operation(summary = "Schedule an interview round")
    public ResponseEntity<InterviewResponse> scheduleInterview(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID applicationId,
            @Valid @RequestBody ScheduleInterviewRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(interviewService.scheduleInterview(principal.getId(), applicationId, req));
    }

    @PutMapping("/recruiters/interviews/{interviewId}")
    @PreAuthorize("hasAuthority('ROLE_RECRUITER')")
    @Operation(summary = "Reschedule an interview")
    public ResponseEntity<InterviewResponse> rescheduleInterview(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID interviewId,
            @Valid @RequestBody UpdateInterviewRequest req) {
        return ResponseEntity.ok(interviewService.rescheduleInterview(principal.getId(), interviewId, req));
    }

    @PatchMapping("/recruiters/interviews/{interviewId}/cancel")
    @PreAuthorize("hasAuthority('ROLE_RECRUITER')")
    @Operation(summary = "Cancel an interview")
    public ResponseEntity<InterviewResponse> cancelInterview(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID interviewId,
            @Valid @RequestBody CancelInterviewRequest req) {
        return ResponseEntity.ok(interviewService.cancelInterview(principal.getId(), interviewId, req));
    }

    @PatchMapping("/recruiters/interviews/{interviewId}/complete")
    @PreAuthorize("hasAuthority('ROLE_RECRUITER')")
    @Operation(summary = "Mark an interview as completed")
    public ResponseEntity<InterviewResponse> completeInterview(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID interviewId) {
        return ResponseEntity.ok(interviewService.completeInterview(principal.getId(), interviewId));
    }
}
