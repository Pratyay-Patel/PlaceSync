package com.placesync.common.spec;

import com.placesync.job.dto.JobFilterRequest;
import com.placesync.job.entity.Job;
import com.placesync.job.entity.JobRequiredSkill;
import com.placesync.job.entity.JobStatus;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

public class JobSpecification {

    private JobSpecification() {}

    public static Specification<Job> withFilters(JobFilterRequest filter) {
        return Specification.where(statusIsOpen())
                .and(notDeleted())
                .and(keywordMatches(filter.getKeyword()))
                .and(companyMatches(filter.getCompanyId()))
                .and(locationTypeMatches(filter.getLocationType()))
                .and(jobTypeMatches(filter.getJobType()))
                .and(skillMatches(filter.getSkill()))
                .and(deadlineAfter(filter.getDeadlineAfter()));
    }

    private static Specification<Job> statusIsOpen() {
        return (root, query, cb) -> cb.equal(root.get("status"), JobStatus.OPEN);
    }

    private static Specification<Job> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    private static Specification<Job> keywordMatches(String keyword) {
        if (keyword == null || keyword.isBlank()) return null;
        String pattern = "%" + keyword.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), pattern),
                cb.like(cb.lower(root.get("description")), pattern)
        );
    }

    private static Specification<Job> companyMatches(java.util.UUID companyId) {
        if (companyId == null) return null;
        return (root, query, cb) -> cb.equal(root.get("company").get("id"), companyId);
    }

    private static Specification<Job> locationTypeMatches(com.placesync.job.entity.JobLocationType locationType) {
        if (locationType == null) return null;
        return (root, query, cb) -> cb.equal(root.get("locationType"), locationType);
    }

    private static Specification<Job> jobTypeMatches(com.placesync.job.entity.JobType jobType) {
        if (jobType == null) return null;
        return (root, query, cb) -> cb.equal(root.get("jobType"), jobType);
    }

    private static Specification<Job> skillMatches(String skill) {
        if (skill == null || skill.isBlank()) return null;
        String pattern = "%" + skill.toLowerCase() + "%";
        return (root, query, cb) -> {
            var skillJoin = root.<Job, JobRequiredSkill>join("requiredSkills", JoinType.LEFT);
            query.distinct(true);
            return cb.like(cb.lower(skillJoin.get("skillName")), pattern);
        };
    }

    private static Specification<Job> deadlineAfter(java.time.OffsetDateTime deadline) {
        if (deadline == null) return null;
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("applicationDeadline"), deadline);
    }
}
