package com.placesync.analytics.dto;

public record DepartmentStatsResponse(
        String department,
        long placedCount,
        long totalStudents,
        double placementRate
) {}
