package com.placesync.recruiter.dto;

import com.placesync.recruiter.entity.RecruiterProfile;
import com.placesync.recruiter.entity.VerificationStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
public class RecruiterProfileResponse {

    private UUID id;
    private UUID userId;
    private String firstName;
    private String lastName;
    private String jobTitle;
    private String contactEmail;
    private String phone;
    private UUID companyId;
    private String companyName;
    private VerificationStatus verificationStatus;
    private OffsetDateTime verifiedAt;
    private String rejectionReason;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static RecruiterProfileResponse from(RecruiterProfile p) {
        return RecruiterProfileResponse.builder()
                .id(p.getId())
                .userId(p.getUser().getId())
                .firstName(p.getFirstName())
                .lastName(p.getLastName())
                .jobTitle(p.getJobTitle())
                .contactEmail(p.getContactEmail())
                .phone(p.getPhone())
                .companyId(p.getCompany() != null ? p.getCompany().getId() : null)
                .companyName(p.getCompany() != null ? p.getCompany().getName() : null)
                .verificationStatus(p.getVerificationStatus())
                .verifiedAt(p.getVerifiedAt())
                .rejectionReason(p.getRejectionReason())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
