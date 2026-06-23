package com.placesync.recruiter.repository;

import com.placesync.recruiter.entity.RecruiterProfile;
import com.placesync.recruiter.entity.VerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RecruiterProfileRepository extends JpaRepository<RecruiterProfile, UUID> {

    Optional<RecruiterProfile> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

    Page<RecruiterProfile> findByVerificationStatus(VerificationStatus status, Pageable pageable);

    Page<RecruiterProfile> findByCompanyId(UUID companyId, Pageable pageable);
}
