package com.placesync.user.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
public class StudentEducationResponse {
    private UUID id;
    private String degree;
    private String institution;
    private String fieldOfStudy;
    private Short startYear;
    private Short endYear;
    private BigDecimal percentageOrCgpa;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
