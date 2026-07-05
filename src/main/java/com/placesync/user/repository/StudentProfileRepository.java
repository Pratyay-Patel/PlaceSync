package com.placesync.user.repository;

import com.placesync.user.entity.StudentProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentProfileRepository extends JpaRepository<StudentProfile, UUID> {

    Optional<StudentProfile> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

    Page<StudentProfile> findByDepartment(String department, Pageable pageable);

    Page<StudentProfile> findByInstitution(String institution, Pageable pageable);

    @Query(value = "SELECT department, COUNT(id) FROM student_profiles WHERE department IS NOT NULL GROUP BY department", nativeQuery = true)
    List<Object[]> countStudentsByDepartment();
}
