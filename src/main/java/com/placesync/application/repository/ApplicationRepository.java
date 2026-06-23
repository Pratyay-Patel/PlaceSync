package com.placesync.application.repository;

import com.placesync.application.entity.Application;
import com.placesync.application.entity.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    Optional<Application> findByStudentIdAndJobId(UUID studentId, UUID jobId);

    boolean existsByStudentIdAndJobId(UUID studentId, UUID jobId);

    Page<Application> findByStudentId(UUID studentId, Pageable pageable);

    Page<Application> findByJobId(UUID jobId, Pageable pageable);

    Page<Application> findByJobIdAndStatus(UUID jobId, ApplicationStatus status, Pageable pageable);
}
