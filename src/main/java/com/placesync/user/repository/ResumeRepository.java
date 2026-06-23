package com.placesync.user.repository;

import com.placesync.user.entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, UUID> {

    List<Resume> findByStudentIdAndDeletedAtIsNullOrderByUploadedAtDesc(UUID studentId);

    Optional<Resume> findByStudentIdAndIsDefaultTrueAndDeletedAtIsNull(UUID studentId);

    boolean existsByS3Key(String s3Key);
}
