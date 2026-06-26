package com.placesync.interview.dto;

import com.placesync.interview.entity.InterviewType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
public class ScheduleInterviewRequest {

    @NotNull
    @Positive
    private Short roundNumber;

    @NotNull
    private InterviewType interviewType;

    @NotNull
    @Future
    private OffsetDateTime scheduledAt;

    @NotNull
    @Positive
    private Short durationMinutes;

    private String meetingLink;

    private String venue;
}
