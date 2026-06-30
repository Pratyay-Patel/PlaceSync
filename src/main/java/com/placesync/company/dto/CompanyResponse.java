package com.placesync.company.dto;

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

}
