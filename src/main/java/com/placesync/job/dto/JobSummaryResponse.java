package com.placesync.job.dto;

import com.placesync.job.entity.Job;
import com.placesync.job.entity.JobLocationType;
import com.placesync.job.entity.JobStatus;
import com.placesync.job.entity.JobType;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobSummaryResponse {

    private UUID id;
    private String title;
    private UUID companyId;
    private String companyName;
    private JobLocationType locationType;
    private JobType jobType;
    private String locationCity;
    private String compensation;
    private OffsetDateTime applicationDeadline;
    private BigDecimal minCgpa;
    private JobStatus status;
    private OffsetDateTime createdAt;

    public static JobSummaryResponse from(Job job) {
        return JobSummaryResponse.builder()
                .id(job.getId())
                .title(job.getTitle())
                .companyId(job.getCompany().getId())
                .companyName(job.getCompany().getName())
                .locationType(job.getLocationType())
                .jobType(job.getJobType())
                .locationCity(job.getLocationCity())
                .compensation(job.getCompensation())
                .applicationDeadline(job.getApplicationDeadline())
                .minCgpa(job.getMinCgpa())
                .status(job.getStatus())
                .createdAt(job.getCreatedAt())
                .build();
    }
}
