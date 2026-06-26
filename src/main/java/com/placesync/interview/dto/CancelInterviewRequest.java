package com.placesync.interview.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CancelInterviewRequest {

    @NotBlank
    private String cancellationReason;
}
