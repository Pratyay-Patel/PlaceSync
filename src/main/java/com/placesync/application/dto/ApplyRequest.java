package com.placesync.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ApplyRequest {

    @NotNull
    private UUID jobId;

    @NotNull
    private UUID resumeId;
}
