package com.placesync.job.dto;

import com.placesync.job.entity.JobLocationType;
import com.placesync.job.entity.JobType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
public class JobFilterRequest {

    private String keyword;
    private UUID companyId;
    private JobLocationType locationType;
    private JobType jobType;
    private String skill;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime deadlineAfter;

    public String cacheKey() {
        return (keyword != null ? keyword : "") + "|" +
               (companyId != null ? companyId : "") + "|" +
               (locationType != null ? locationType : "") + "|" +
               (jobType != null ? jobType : "") + "|" +
               (skill != null ? skill : "") + "|" +
               (deadlineAfter != null ? deadlineAfter : "");
    }
}
