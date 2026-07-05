package com.placesync.analytics.dto;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public record RecruiterStatsResponse(
        long jobsPosted,
        long totalApplications,
        long shortlisted,
        long offers
) {}
