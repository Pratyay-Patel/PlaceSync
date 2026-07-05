package com.placesync.analytics.dto;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public record DepartmentStatsResponse(
        String department,
        long placedCount,
        long totalStudents,
        double placementRate
) {}
