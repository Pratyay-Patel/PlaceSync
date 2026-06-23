package com.placesync.company.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompanyVerificationRequest {

    @NotNull
    private Decision decision;

    private String rejectionReason;

    public enum Decision { APPROVE, REJECT }
}
