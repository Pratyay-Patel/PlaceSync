package com.placesync.analytics.dto;

import java.util.UUID;

public record CompanyStatsResponse(
        UUID companyId,
        String companyName,
        long offerCount,
        long jobCount,
        long applicationCount
) {}
