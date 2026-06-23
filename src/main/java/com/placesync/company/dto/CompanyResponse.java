package com.placesync.company.dto;

import com.placesync.company.entity.Company;
import com.placesync.company.entity.CompanyStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
public class CompanyResponse {

    private UUID id;
    private String name;
    private String description;
    private String websiteUrl;
    private String industry;
    private String headquarters;
    private CompanyStatus status;
    private UUID createdById;
    private OffsetDateTime verifiedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static CompanyResponse from(Company c) {
        return CompanyResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .description(c.getDescription())
                .websiteUrl(c.getWebsiteUrl())
                .industry(c.getIndustry())
                .headquarters(c.getHeadquarters())
                .status(c.getStatus())
                .createdById(c.getCreatedBy().getId())
                .verifiedAt(c.getVerifiedAt())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
