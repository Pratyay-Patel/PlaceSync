package com.placesync.analytics.dto;

public record RecruiterStatsResponse(
        long jobsPosted,
        long totalApplications,
        long shortlisted,
        long offers
) {}
