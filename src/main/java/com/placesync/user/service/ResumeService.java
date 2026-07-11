package com.placesync.user.service;

import com.placesync.application.repository.ApplicationRepository;
import com.placesync.common.exception.ResourceNotFoundException;
import com.placesync.common.storage.FileValidationService;
import com.placesync.common.storage.S3StorageService;
import com.placesync.recruiter.repository.RecruiterProfileRepository;
import com.placesync.user.dto.ResumeDownloadUrlResponse;
import com.placesync.user.dto.ResumeResponse;
import com.placesync.user.entity.Resume;
import com.placesync.user.entity.StudentProfile;
import com.placesync.user.entity.UserRole;
import com.placesync.user.mapper.ResumeMapper;
import com.placesync.user.repository.ResumeRepository;
import com.placesync.user.repository.StudentProfileRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static com.placesync.common.util.LogSanitizer.sanitize;

@SuppressWarnings("java:S2629")
@Service
@RequiredArgsConstructor
public class ResumeService {

    private static final Logger log = LoggerFactory.getLogger(ResumeService.class);
    private static final String STUDENT_PROFILE = "StudentProfile";
    private static final String RESUME = "Resume";

    private final ResumeRepository resumeRepository;
    private final ResumeMapper resumeMapper;
    private final StudentProfileRepository studentProfileRepository;
    private final RecruiterProfileRepository recruiterProfileRepository;
    private final ApplicationRepository applicationRepository;
    private final S3StorageService s3StorageService;
    private final FileValidationService fileValidationService;

    @Value("${app.aws.bucket-resumes}")
    private String resumesBucket;

    @Transactional(readOnly = true)
    public List<ResumeResponse> getMyResumes(UUID userId) {
        StudentProfile student = studentProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(STUDENT_PROFILE, userId));
        return resumeRepository.findByStudentIdAndDeletedAtIsNullOrderByUploadedAtDesc(student.getId())
                .stream()
                .map(resumeMapper::toResponse)
                .toList();
    }

    @Transactional
    public ResumeResponse uploadResume(UUID userId, MultipartFile file, String label, boolean isDefault) {
        log.info("Uploading resume for userId={}", sanitize(userId));
        fileValidationService.validatePdf(file);

        StudentProfile student = studentProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(STUDENT_PROFILE, userId));

        if (isDefault) {
            resumeRepository.findByStudentIdAndIsDefaultTrueAndDeletedAtIsNull(student.getId())
                    .ifPresent(existing -> {
                        existing.setIsDefault(false);
                        resumeRepository.save(existing);
                    });
            resumeRepository.flush();
        }

        UUID resumeId = UUID.randomUUID();
        String s3Key = buildResumeKey(student.getId(), resumeId, file.getOriginalFilename());

        try {
            s3StorageService.uploadFile(resumesBucket, s3Key,
                    file.getInputStream(), "application/pdf", file.getSize());
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read uploaded file");
        }

        Resume resume = Resume.builder()
                .id(resumeId)
                .student(student)
                .label(label)
                .originalFilename(file.getOriginalFilename())
                .s3Key(s3Key)
                .fileSizeBytes(file.getSize())
                .isDefault(isDefault)
                .build();

        return resumeMapper.toResponse(resumeRepository.save(resume));
    }

    @Transactional(readOnly = true)
    public ResumeDownloadUrlResponse getDownloadUrl(UUID callerId, UUID resumeId, UserRole callerRole) {
        log.info("Generating download URL for resumeId={} callerId={}", sanitize(resumeId), sanitize(callerId));
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException(RESUME, resumeId));

        if (resume.getDeletedAt() != null) {
            throw new ResourceNotFoundException(RESUME, resumeId);
        }

        if (callerRole == UserRole.ROLE_STUDENT) {
            StudentProfile caller = studentProfileRepository.findByUserId(callerId)
                    .orElseThrow(() -> new ResourceNotFoundException(STUDENT_PROFILE, callerId));
            if (!resume.getStudent().getId().equals(caller.getId())) {
                throw new AccessDeniedException("Resume does not belong to you");
            }
        } else if (callerRole == UserRole.ROLE_RECRUITER) {
            var recruiterProfile = recruiterProfileRepository.findByUserId(callerId)
                    .orElseThrow(() -> new ResourceNotFoundException("RecruiterProfile", callerId));
            long count = applicationRepository.countByStudentAndRecruiter(
                    resume.getStudent().getId(), recruiterProfile.getId());
            if (count == 0) {
                throw new AccessDeniedException("Student has not applied to any of your jobs");
            }
        }

        int expiryMinutes = 15;
        String url = s3StorageService.generatePresignedGetUrl(resumesBucket, resume.getS3Key(), expiryMinutes);
        return new ResumeDownloadUrlResponse(url, OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(expiryMinutes));
    }

    @Transactional
    public ResumeResponse setDefault(UUID userId, UUID resumeId) {
        log.info("Setting resumeId={} as default for userId={}", sanitize(resumeId), sanitize(userId));
        StudentProfile student = studentProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(STUDENT_PROFILE, userId));

        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException(RESUME, resumeId));

        if (!resume.getStudent().getId().equals(student.getId())) {
            throw new AccessDeniedException("Resume does not belong to the student");
        }
        if (resume.getDeletedAt() != null) {
            throw new ResourceNotFoundException(RESUME, resumeId);
        }

        resumeRepository.findByStudentIdAndIsDefaultTrueAndDeletedAtIsNull(student.getId())
                .ifPresent(existing -> {
                    existing.setIsDefault(false);
                    resumeRepository.save(existing);
                });
        resumeRepository.flush();

        resume.setIsDefault(true);
        return resumeMapper.toResponse(resumeRepository.save(resume));
    }

    @Transactional
    public void softDelete(UUID userId, UUID resumeId) {
        log.info("Soft-deleting resumeId={} for userId={}", sanitize(resumeId), sanitize(userId));
        StudentProfile student = studentProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(STUDENT_PROFILE, userId));

        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException(RESUME, resumeId));

        if (!resume.getStudent().getId().equals(student.getId())) {
            throw new AccessDeniedException("Resume does not belong to the student");
        }
        if (resume.getDeletedAt() != null) {
            throw new ResourceNotFoundException(RESUME, resumeId);
        }

        boolean wasDefault = Boolean.TRUE.equals(resume.getIsDefault());
        resume.setDeletedAt(OffsetDateTime.now(ZoneOffset.UTC));
        resume.setIsDefault(false);
        resumeRepository.save(resume);

        if (wasDefault) {
            resumeRepository
                    .findFirstByStudentIdAndIdNotAndDeletedAtIsNullOrderByUploadedAtDesc(student.getId(), resumeId)
                    .ifPresent(next -> {
                        next.setIsDefault(true);
                        resumeRepository.save(next);
                        log.info("Promoted resumeId={} as new default for studentId={}", sanitize(next.getId()), sanitize(student.getId()));
                    });
        }
    }

    private String buildResumeKey(UUID studentId, UUID resumeId, String originalFilename) {
        String safe = (originalFilename != null)
                ? originalFilename.replaceAll("[^a-zA-Z0-9._-]", "-")
                : resumeId + ".pdf";
        if (!safe.toLowerCase().endsWith(".pdf")) {
            safe = safe.replaceAll("\\.[^.]*$", "") + ".pdf";
        }
        return "resumes/" + studentId + "/" + resumeId + "/" + safe;
    }
}
