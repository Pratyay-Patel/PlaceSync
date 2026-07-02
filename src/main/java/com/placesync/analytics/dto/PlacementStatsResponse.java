package com.placesync.analytics.dto;

public record PlacementStatsResponse(
        long totalStudents,
        long totalRecruiters,
        long totalCompanies,
        long openJobs,
        long totalApplications,
        long totalOffers,
        double placementRate
) {}
