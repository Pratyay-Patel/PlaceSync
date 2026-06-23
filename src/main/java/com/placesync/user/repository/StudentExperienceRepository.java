package com.placesync.user.repository;

import com.placesync.user.entity.StudentExperience;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StudentExperienceRepository extends JpaRepository<StudentExperience, UUID> {

    List<StudentExperience> findByStudentIdOrderByStartDateDesc(UUID studentId);
}
