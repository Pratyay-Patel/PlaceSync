package com.placesync.job.dto;

import com.placesync.common.validation.FutureDate;
import com.placesync.job.entity.JobLocationType;
import com.placesync.job.entity.JobType;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class CreateJobRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String description;

    @NotNull
    private JobLocationType locationType;

    @NotNull
    private JobType jobType;

    private String locationCity;

    private String compensation;

    @NotNull
    @FutureDate
    private OffsetDateTime applicationDeadline;

    @DecimalMin("0.0")
    @DecimalMax("10.0")
    private BigDecimal minCgpa;

    private List<String> requiredSkills = new ArrayList<>();

    private List<String> eligibleDepartments = new ArrayList<>();
}
