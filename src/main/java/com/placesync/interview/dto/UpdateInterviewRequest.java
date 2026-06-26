package com.placesync.interview.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
public class UpdateInterviewRequest {

    @NotNull
    @Future
    private OffsetDateTime scheduledAt;

    private String meetingLink;

    private String venue;
}
