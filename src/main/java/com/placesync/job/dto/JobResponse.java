package com.placesync.job.dto;

import com.placesync.job.entity.Job;
import com.placesync.job.entity.JobLocationType;
import com.placesync.job.entity.JobStatus;
import com.placesync.job.entity.JobType;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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

    public static JobResponse from(Job job) {
        return JobResponse.builder()
                .id(job.getId())
                .title(job.getTitle())
                .description(job.getDescription())
                .companyId(job.getCompany().getId())
                .companyName(job.getCompany().getName())
                .recruiterId(job.getRecruiter().getId())
                .recruiterFirstName(job.getRecruiter().getFirstName())
                .recruiterLastName(job.getRecruiter().getLastName())
                .locationType(job.getLocationType())
                .jobType(job.getJobType())
                .locationCity(job.getLocationCity())
                .compensation(job.getCompensation())
                .applicationDeadline(job.getApplicationDeadline())
                .minCgpa(job.getMinCgpa())
                .status(job.getStatus())
                .requiredSkills(job.getRequiredSkills().stream()
                        .map(s -> s.getSkillName())
                        .collect(Collectors.toList()))
                .eligibleDepartments(job.getEligibleDepartments().stream()
                        .map(d -> d.getDepartmentName())
                        .collect(Collectors.toList()))
                .approvedAt(job.getApprovedAt())
                .closedAt(job.getClosedAt())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }
}
