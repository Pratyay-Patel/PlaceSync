package com.placesync.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class StudentExperienceRequest {

    @NotBlank
    @Size(max = 255)
    private String companyName;

    @NotBlank
    @Size(max = 255)
    private String role;

    @Size(max = 5000)
    private String description;

    @NotNull
    private LocalDate startDate;

    private LocalDate endDate;

    private Boolean isCurrent = false;
}
