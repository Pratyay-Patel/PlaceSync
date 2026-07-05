package com.placesync.analytics.dto;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.UUID;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public record CompanyStatsResponse(
        UUID companyId,
        String companyName,
        long offerCount,
        long jobCount,
        long applicationCount
) {}
