package com.placesync.user.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
public class StudentExperienceResponse {
    private UUID id;
    private String companyName;
    private String role;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean isCurrent;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
