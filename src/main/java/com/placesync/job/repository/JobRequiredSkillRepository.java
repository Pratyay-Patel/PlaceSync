package com.placesync.job.repository;

import com.placesync.job.entity.JobRequiredSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JobRequiredSkillRepository extends JpaRepository<JobRequiredSkill, UUID> {

    List<JobRequiredSkill> findByJobId(UUID jobId);

    void deleteByJobId(UUID jobId);
}
