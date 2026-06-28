package com.placesync.common.spec;

import com.placesync.application.entity.Application;
import com.placesync.application.entity.ApplicationStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public class ApplicationSpecification {

    private ApplicationSpecification() {}

    public static Specification<Application> withFilters(UUID jobId, UUID studentId, ApplicationStatus status) {
        return Specification.where(jobIdMatches(jobId))
                .and(studentIdMatches(studentId))
                .and(statusMatches(status));
    }

    private static Specification<Application> jobIdMatches(UUID jobId) {
        if (jobId == null) return null;
        return (root, query, cb) -> cb.equal(root.get("job").get("id"), jobId);
    }

    private static Specification<Application> studentIdMatches(UUID studentId) {
        if (studentId == null) return null;
        return (root, query, cb) -> cb.equal(root.get("student").get("id"), studentId);
    }

    private static Specification<Application> statusMatches(ApplicationStatus status) {
        if (status == null) return null;
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }
}
