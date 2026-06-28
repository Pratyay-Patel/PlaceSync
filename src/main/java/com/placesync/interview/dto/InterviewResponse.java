package com.placesync.interview.dto;

import com.placesync.interview.entity.InterviewStatus;
import com.placesync.interview.entity.InterviewType;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewResponse {

    private UUID id;
    private UUID applicationId;
    private UUID studentId;
    private String studentFirstName;
    private String studentLastName;
    private UUID jobId;
    private String jobTitle;
    private String companyName;
    private Short roundNumber;
    private InterviewType interviewType;
    private InterviewStatus status;
    private OffsetDateTime scheduledAt;
    private Short durationMinutes;
    private String meetingLink;
    private String venue;
    private String cancellationReason;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

}
