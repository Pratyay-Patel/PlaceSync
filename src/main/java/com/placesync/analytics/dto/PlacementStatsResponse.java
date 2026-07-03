package com.placesync.analytics.dto;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public record PlacementStatsResponse(
        long totalStudents,
        long totalRecruiters,
        long totalCompanies,
        long openJobs,
        long totalApplications,
        long totalOffers,
        double placementRate
) {}
