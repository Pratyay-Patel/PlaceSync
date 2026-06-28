package com.placesync.application.service;

import com.placesync.application.dto.ApplyRequest;
import com.placesync.application.dto.ApplicationResponse;
import com.placesync.application.dto.UpdateApplicationStatusRequest;
import com.placesync.application.mapper.ApplicationMapper;
import com.placesync.application.entity.Application;
import com.placesync.application.entity.ApplicationStatus;
import com.placesync.application.repository.ApplicationRepository;
import com.placesync.common.audit.AuditAction;
import com.placesync.common.audit.Auditable;
import com.placesync.common.exception.ConflictException;
import com.placesync.common.exception.ResourceNotFoundException;
import com.placesync.common.util.PagedResponse;
import com.placesync.job.entity.Job;
import com.placesync.job.entity.JobStatus;
import com.placesync.job.repository.JobRepository;
import com.placesync.recruiter.entity.RecruiterProfile;
import com.placesync.recruiter.repository.RecruiterProfileRepository;
import com.placesync.user.entity.Resume;
import com.placesync.user.entity.StudentProfile;
import com.placesync.user.repository.ResumeRepository;
import com.placesync.user.repository.StudentProfileRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private static final Logger log = LoggerFactory.getLogger(ApplicationService.class);
    private static final String STUDENT_PROFILE = "StudentProfile";

    private static final Map<ApplicationStatus, Set<ApplicationStatus>> VALID_TRANSITIONS = Map.of(
            ApplicationStatus.APPLIED,              Set.of(ApplicationStatus.UNDER_REVIEW, ApplicationStatus.REJECTED),
            ApplicationStatus.UNDER_REVIEW,         Set.of(ApplicationStatus.SHORTLISTED, ApplicationStatus.REJECTED),
            ApplicationStatus.SHORTLISTED,          Set.of(ApplicationStatus.INTERVIEW_SCHEDULED, ApplicationStatus.REJECTED),
            ApplicationStatus.INTERVIEW_SCHEDULED,  Set.of(ApplicationStatus.OFFERED, ApplicationStatus.REJECTED),
            ApplicationStatus.OFFERED,              Set.of(),
            ApplicationStatus.REJECTED,             Set.of()
    );

    private final ApplicationRepository applicationRepository;
    private final ApplicationMapper applicationMapper;
    private final StudentProfileRepository studentProfileRepository;
    private final JobRepository jobRepository;
    private final ResumeRepository resumeRepository;
    private final RecruiterProfileRepository recruiterProfileRepository;

    @Auditable(action = AuditAction.CREATE, entityType = "Application")
    @Transactional
    public ApplicationResponse apply(UUID userId, ApplyRequest req) {
        log.info("Student userId={} applying to jobId={}", userId, req.getJobId());
        StudentProfile student = studentProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(STUDENT_PROFILE, userId));

        Job job = jobRepository.findByIdAndDeletedAtIsNull(req.getJobId())
                .orElseThrow(() -> new ResourceNotFoundException("Job", req.getJobId()));

        Resume resume = resumeRepository.findById(req.getResumeId())
                .orElseThrow(() -> new ResourceNotFoundException("Resume", req.getResumeId()));

        if (job.getStatus() != JobStatus.OPEN) {
            throw new ConflictException("Job is not currently accepting applications");
        }
        if (!resume.getStudent().getId().equals(student.getId())) {
            throw new AccessDeniedException("Resume does not belong to the student");
        }
        if (resume.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Resume", req.getResumeId());
        }
        if (applicationRepository.existsByStudentIdAndJobId(student.getId(), job.getId())) {
            throw new ConflictException("You have already applied to this job");
        }

        if (job.getMinCgpa() != null && student.getCgpa() != null
                && student.getCgpa().compareTo(job.getMinCgpa()) < 0) {
            throw new ConflictException(
                    "Your CGPA (" + student.getCgpa() + ") does not meet the minimum requirement of " + job.getMinCgpa());
        }

        if (!job.getEligibleDepartments().isEmpty()) {
            boolean eligible = job.getEligibleDepartments().stream()
                    .anyMatch(d -> d.getDepartmentName().equalsIgnoreCase(student.getDepartment()));
            if (!eligible) {
                throw new ConflictException("Your department (" + student.getDepartment() + ") is not eligible for this job");
            }
        }

        Application application = Application.builder()
                .student(student)
                .job(job)
                .resume(resume)
                .status(ApplicationStatus.APPLIED)
                .build();

        return applicationMapper.toResponse(applicationRepository.save(application));
    }

    @Transactional(readOnly = true)
    public PagedResponse<ApplicationResponse> getMyApplications(UUID userId, Pageable pageable) {
        StudentProfile student = studentProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(STUDENT_PROFILE, userId));
        return PagedResponse.of(
                applicationRepository.findByStudentId(student.getId(), pageable)
                        .map(applicationMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public ApplicationResponse getMyApplication(UUID userId, UUID applicationId) {
        StudentProfile student = studentProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(STUDENT_PROFILE, userId));

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", applicationId));

        if (!application.getStudent().getId().equals(student.getId())) {
            throw new AccessDeniedException("Application does not belong to the student");
        }
        return applicationMapper.toResponse(application);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ApplicationResponse> getJobApplications(UUID userId, UUID jobId, Pageable pageable) {
        RecruiterProfile recruiter = recruiterProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("RecruiterProfile", userId));

        Job job = jobRepository.findByIdAndDeletedAtIsNull(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", jobId));

        if (!job.getRecruiter().getId().equals(recruiter.getId())) {
            throw new AccessDeniedException("Job does not belong to the recruiter");
        }
        return PagedResponse.of(
                applicationRepository.findByJobId(jobId, pageable)
                        .map(applicationMapper::toResponse));
    }

    @Auditable(action = AuditAction.UPDATE, entityType = "Application")
    @Transactional
    public ApplicationResponse updateStatus(UUID userId, UUID applicationId, UpdateApplicationStatusRequest req) {
        log.info("Updating applicationId={} status to {} by userId={}", applicationId, req.getStatus(), userId);
        RecruiterProfile recruiter = recruiterProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("RecruiterProfile", userId));

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", applicationId));

        if (!application.getJob().getRecruiter().getId().equals(recruiter.getId())) {
            throw new AccessDeniedException("Application does not belong to the recruiter's job");
        }

        ApplicationStatus current = application.getStatus();
        ApplicationStatus next = req.getStatus();
        if (!VALID_TRANSITIONS.getOrDefault(current, Set.of()).contains(next)) {
            throw new IllegalArgumentException(
                    "Invalid status transition from " + current + " to " + next);
        }

        application.setStatus(next);
        return applicationMapper.toResponse(applicationRepository.save(application));
    }
}
