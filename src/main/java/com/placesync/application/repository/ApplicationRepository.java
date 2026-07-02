package com.placesync.application.repository;

import com.placesync.application.entity.Application;
import com.placesync.application.entity.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, UUID>, JpaSpecificationExecutor<Application> {

    Optional<Application> findByStudentIdAndJobId(UUID studentId, UUID jobId);

    boolean existsByStudentIdAndJobId(UUID studentId, UUID jobId);

    Page<Application> findByStudentId(UUID studentId, Pageable pageable);

    Page<Application> findByJobId(UUID jobId, Pageable pageable);

    Page<Application> findByJobIdAndStatus(UUID jobId, ApplicationStatus status, Pageable pageable);

    long countByStatus(ApplicationStatus status);

    @Query(value = """
            SELECT j.company_id, c.name,
                   COALESCE(SUM(CASE WHEN a.status = 'OFFERED' THEN 1 ELSE 0 END), 0) AS offer_count,
                   COUNT(DISTINCT j.id) AS job_count,
                   COUNT(a.id) AS application_count
            FROM jobs j
            INNER JOIN companies c ON c.id = j.company_id
            LEFT JOIN applications a ON a.job_id = j.id
            WHERE c.deleted_at IS NULL AND j.deleted_at IS NULL
            GROUP BY j.company_id, c.name
            ORDER BY offer_count DESC
            LIMIT 10
            """, nativeQuery = true)
    List<Object[]> findCompanyBreakdown();

    @Query(value = """
            SELECT sp.department, COUNT(DISTINCT a.student_id)
            FROM applications a
            INNER JOIN student_profiles sp ON sp.id = a.student_id
            WHERE a.status = 'OFFERED' AND sp.department IS NOT NULL
            GROUP BY sp.department
            """, nativeQuery = true)
    List<Object[]> countOfferedByDepartment();

    @Query(value = """
            SELECT COUNT(a.id),
                   SUM(CASE WHEN a.status = 'SHORTLISTED' THEN 1 ELSE 0 END),
                   SUM(CASE WHEN a.status = 'OFFERED' THEN 1 ELSE 0 END)
            FROM applications a
            INNER JOIN jobs j ON j.id = a.job_id
            WHERE j.recruiter_id = :recruiterId
            """, nativeQuery = true)
    Object[] findRecruiterApplicationStats(@Param("recruiterId") UUID recruiterId);
}
