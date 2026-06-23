package com.placesync.job.repository;

import com.placesync.job.entity.JobEligibleDepartment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JobEligibleDepartmentRepository extends JpaRepository<JobEligibleDepartment, UUID> {

    List<JobEligibleDepartment> findByJobId(UUID jobId);

    boolean existsByJobIdAndDepartmentName(UUID jobId, String departmentName);

    void deleteByJobId(UUID jobId);
}
