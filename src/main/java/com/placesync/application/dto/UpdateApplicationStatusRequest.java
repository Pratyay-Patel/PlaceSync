package com.placesync.application.dto;

import com.placesync.application.entity.ApplicationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateApplicationStatusRequest {

    @NotNull
    private ApplicationStatus status;

    private String note;
}
