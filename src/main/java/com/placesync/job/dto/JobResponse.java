package com.placesync.job.dto;

import com.placesync.job.entity.JobLocationType;
import com.placesync.job.entity.JobStatus;
import com.placesync.job.entity.JobType;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobResponse {

    private UUID id;
    private String title;
    private String description;
    private UUID companyId;
    private String companyName;
    private UUID recruiterId;
    private String recruiterFirstName;
    private String recruiterLastName;
    private JobLocationType locationType;
    private JobType jobType;
    private String locationCity;
    private String compensation;
    private OffsetDateTime applicationDeadline;
    private BigDecimal minCgpa;
    private JobStatus status;
    private List<String> requiredSkills;
    private List<String> eligibleDepartments;
    private OffsetDateTime approvedAt;
    private OffsetDateTime closedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

}
