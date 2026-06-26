package com.placesync.job.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JobApprovalRequest {

    public enum Decision { APPROVE, REJECT }

    @NotNull
    private Decision decision;

    private String rejectionReason;
}
