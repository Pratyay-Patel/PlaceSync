package com.placesync.interview.dto;

import com.placesync.interview.entity.Interview;
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

    public static InterviewResponse from(Interview interview) {
        var app = interview.getApplication();
        var student = app.getStudent();
        var job = app.getJob();
        return InterviewResponse.builder()
                .id(interview.getId())
                .applicationId(app.getId())
                .studentId(student.getId())
                .studentFirstName(student.getFirstName())
                .studentLastName(student.getLastName())
                .jobId(job.getId())
                .jobTitle(job.getTitle())
                .companyName(job.getCompany().getName())
                .roundNumber(interview.getRoundNumber())
                .interviewType(interview.getInterviewType())
                .status(interview.getStatus())
                .scheduledAt(interview.getScheduledAt())
                .durationMinutes(interview.getDurationMinutes())
                .meetingLink(interview.getMeetingLink())
                .venue(interview.getVenue())
                .cancellationReason(interview.getCancellationReason())
                .createdAt(interview.getCreatedAt())
                .updatedAt(interview.getUpdatedAt())
                .build();
    }
}
