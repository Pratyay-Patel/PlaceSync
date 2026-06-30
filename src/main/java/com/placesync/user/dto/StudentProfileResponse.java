package com.placesync.user.dto;

import com.placesync.user.entity.GenderType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
public class StudentProfileResponse {

    private UUID id;
    private UUID userId;
    private String firstName;
    private String lastName;
    private String phone;
    private LocalDate dateOfBirth;
    private GenderType gender;
    private String institution;
    private String department;
    private Short graduationYear;
    private BigDecimal cgpa;
    private String bio;
    private boolean profilePublic;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

}
