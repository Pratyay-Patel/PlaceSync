package com.placesync.analytics.controller;

import com.placesync.analytics.dto.CompanyStatsResponse;
import com.placesync.analytics.dto.DepartmentStatsResponse;
import com.placesync.analytics.dto.PlacementStatsResponse;
import com.placesync.analytics.dto.RecruiterStatsResponse;
import com.placesync.analytics.service.AnalyticsService;
import com.placesync.common.util.ApiResponse;
import com.placesync.common.security.UserPrincipal;
import com.placesync.common.util.ApiResponseFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/placement-stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PlacementStatsResponse>> getPlacementStats() {
        return ResponseEntity.ok(ApiResponseFactory.ok(analyticsService.getPlacementStats()));
    }

    @GetMapping("/companies")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<CompanyStatsResponse>>> getCompanyBreakdown() {
        return ResponseEntity.ok(ApiResponseFactory.ok(analyticsService.getCompanyBreakdown()));
    }

    @GetMapping("/departments")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<DepartmentStatsResponse>>> getDepartmentBreakdown() {
        return ResponseEntity.ok(ApiResponseFactory.ok(analyticsService.getDepartmentBreakdown()));
    }

    @GetMapping("/recruiter")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<RecruiterStatsResponse>> getRecruiterStats(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponseFactory.ok(analyticsService.getRecruiterStats(principal.getId())));
    }
}
