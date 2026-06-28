package com.placesync.job.repository;

import com.placesync.job.entity.Job;
import com.placesync.job.entity.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JobRepository extends JpaRepository<Job, UUID>, JpaSpecificationExecutor<Job> {

    Optional<Job> findByIdAndDeletedAtIsNull(UUID id);

    Page<Job> findByStatusAndDeletedAtIsNull(JobStatus status, Pageable pageable);

    Page<Job> findByRecruiterIdAndDeletedAtIsNull(UUID recruiterId, Pageable pageable);

    Page<Job> findByCompanyIdAndDeletedAtIsNull(UUID companyId, Pageable pageable);
}
