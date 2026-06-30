package com.placesync.application.dto;

import com.placesync.application.entity.ApplicationStatus;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationResponse {

    private UUID id;
    private UUID studentId;
    private String studentFirstName;
    private String studentLastName;
    private UUID jobId;
    private String jobTitle;
    private String companyName;
    private UUID resumeId;
    private String resumeLabel;
    private ApplicationStatus status;
    private OffsetDateTime appliedAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime statusUpdatedAt;

}
