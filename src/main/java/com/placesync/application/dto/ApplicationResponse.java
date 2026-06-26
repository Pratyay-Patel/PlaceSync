package com.placesync.application.dto;

import com.placesync.application.entity.Application;
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

    public static ApplicationResponse from(Application app) {
        return ApplicationResponse.builder()
                .id(app.getId())
                .studentId(app.getStudent().getId())
                .studentFirstName(app.getStudent().getFirstName())
                .studentLastName(app.getStudent().getLastName())
                .jobId(app.getJob().getId())
                .jobTitle(app.getJob().getTitle())
                .companyName(app.getJob().getCompany().getName())
                .resumeId(app.getResume().getId())
                .resumeLabel(app.getResume().getLabel())
                .status(app.getStatus())
                .appliedAt(app.getAppliedAt())
                .updatedAt(app.getUpdatedAt())
                .statusUpdatedAt(app.getStatusUpdatedAt())
                .build();
    }
}
