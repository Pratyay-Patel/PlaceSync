package com.placesync.interview.repository;

import com.placesync.interview.entity.Interview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InterviewRepository extends JpaRepository<Interview, UUID> {

    List<Interview> findByApplicationIdOrderByRoundNumberAsc(UUID applicationId);

    boolean existsByApplicationIdAndRoundNumber(UUID applicationId, Short roundNumber);

    List<Interview> findByApplication_StudentIdOrderByScheduledAtAsc(UUID studentId);
}
