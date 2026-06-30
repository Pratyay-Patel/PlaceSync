package com.placesync.user.service;

import com.placesync.common.exception.ResourceNotFoundException;
import com.placesync.user.dto.CreateResumeRequest;
import com.placesync.user.dto.ResumeResponse;
import com.placesync.user.mapper.ResumeMapper;
import com.placesync.user.entity.Resume;
import com.placesync.user.entity.StudentProfile;
import com.placesync.user.repository.ResumeRepository;
import com.placesync.user.repository.StudentProfileRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResumeService {

    private static final Logger log = LoggerFactory.getLogger(ResumeService.class);
    private static final String STUDENT_PROFILE = "StudentProfile";

    private final ResumeRepository resumeRepository;
    private final ResumeMapper resumeMapper;
    private final StudentProfileRepository studentProfileRepository;

    @Transactional(readOnly = true)
    public List<ResumeResponse> getMyResumes(UUID userId) {
        StudentProfile student = studentProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(STUDENT_PROFILE, userId));
        return resumeRepository.findByStudentIdAndDeletedAtIsNullOrderByUploadedAtDesc(student.getId())
                .stream()
                .map(resumeMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ResumeResponse createResume(UUID userId, CreateResumeRequest req) {
        log.info("Creating resume '{}' for userId={}", req.getLabel(), userId);
        StudentProfile student = studentProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(STUDENT_PROFILE, userId));

        if (Boolean.TRUE.equals(req.getIsDefault())) {
            resumeRepository.findByStudentIdAndIsDefaultTrueAndDeletedAtIsNull(student.getId())
                    .ifPresent(existing -> {
                        existing.setIsDefault(false);
                        resumeRepository.save(existing);
                    });
        }

        // Phase 3: placeholder s3Key — replaced by actual S3 key in Phase 5 after file upload
        String placeholderS3Key = "pending/" + student.getId() + "/" + UUID.randomUUID();

        Resume resume = Resume.builder()
                .student(student)
                .label(req.getLabel())
                .originalFilename(req.getOriginalFilename())
                .s3Key(placeholderS3Key)
                .fileSizeBytes(req.getFileSizeBytes())
                .isDefault(Boolean.TRUE.equals(req.getIsDefault()))
                .build();

        return resumeMapper.toResponse(resumeRepository.save(resume));
    }

    @Transactional
    public ResumeResponse setDefault(UUID userId, UUID resumeId) {
        log.info("Setting resumeId={} as default for userId={}", resumeId, userId);
        StudentProfile student = studentProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(STUDENT_PROFILE, userId));

        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume", resumeId));

        if (!resume.getStudent().getId().equals(student.getId())) {
            throw new AccessDeniedException("Resume does not belong to the student");
        }
        if (resume.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Resume", resumeId);
        }

        resumeRepository.findByStudentIdAndIsDefaultTrueAndDeletedAtIsNull(student.getId())
                .ifPresent(existing -> {
                    existing.setIsDefault(false);
                    resumeRepository.save(existing);
                });

        resume.setIsDefault(true);
        return resumeMapper.toResponse(resumeRepository.save(resume));
    }

    @Transactional
    public void softDelete(UUID userId, UUID resumeId) {
        log.info("Soft-deleting resumeId={} for userId={}", resumeId, userId);
        StudentProfile student = studentProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(STUDENT_PROFILE, userId));

        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume", resumeId));

        if (!resume.getStudent().getId().equals(student.getId())) {
            throw new AccessDeniedException("Resume does not belong to the student");
        }
        if (resume.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Resume", resumeId);
        }

        resume.setDeletedAt(OffsetDateTime.now());
        if (Boolean.TRUE.equals(resume.getIsDefault())) {
            resume.setIsDefault(false);
        }
        resumeRepository.save(resume);
    }
}
