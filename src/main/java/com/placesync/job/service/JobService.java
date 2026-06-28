package com.placesync.job.service;

import com.placesync.common.exception.ConflictException;
import com.placesync.common.exception.ResourceNotFoundException;
import com.placesync.common.util.PagedResponse;
import com.placesync.job.dto.*;
import com.placesync.job.entity.*;
import com.placesync.job.mapper.JobMapper;
import com.placesync.job.repository.JobRepository;
import com.placesync.recruiter.entity.RecruiterProfile;
import com.placesync.recruiter.entity.VerificationStatus;
import com.placesync.recruiter.repository.RecruiterProfileRepository;
import com.placesync.user.entity.User;
import com.placesync.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JobService {

    private static final Logger log = LoggerFactory.getLogger(JobService.class);
    private static final String RECRUITER_PROFILE = "RecruiterProfile";

    private final JobRepository jobRepository;
    private final JobMapper jobMapper;
    private final RecruiterProfileRepository recruiterProfileRepository;
    private final UserRepository userRepository;

    @Cacheable(value = "job-listings", key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public PagedResponse<JobSummaryResponse> getOpenJobs(Pageable pageable) {
        return PagedResponse.of(
                jobRepository.findByStatusAndDeletedAtIsNull(JobStatus.OPEN, pageable)
                        .map(jobMapper::toSummaryResponse));
    }

    @Cacheable(value = "job-detail", key = "#jobId")
    @Transactional(readOnly = true)
    public JobResponse getJob(UUID jobId) {
        Job job = jobRepository.findByIdAndDeletedAtIsNull(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", jobId));
        return jobMapper.toResponse(job);
    }

    @Transactional(readOnly = true)
    public PagedResponse<JobSummaryResponse> getRecruiterJobs(UUID userId, Pageable pageable) {
        RecruiterProfile recruiter = recruiterProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(RECRUITER_PROFILE, userId));
        return PagedResponse.of(
                jobRepository.findByRecruiterIdAndDeletedAtIsNull(recruiter.getId(), pageable)
                        .map(jobMapper::toSummaryResponse));
    }

    @Transactional(readOnly = true)
    public PagedResponse<JobSummaryResponse> getPendingJobs(Pageable pageable) {
        return PagedResponse.of(
                jobRepository.findByStatusAndDeletedAtIsNull(JobStatus.PENDING_APPROVAL, pageable)
                        .map(jobMapper::toSummaryResponse));
    }

    @CacheEvict(value = "job-listings", allEntries = true)
    @Transactional
    public JobResponse createJob(UUID userId, CreateJobRequest req) {
        log.info("Creating job '{}' by userId={}", req.getTitle(), userId);
        RecruiterProfile recruiter = recruiterProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(RECRUITER_PROFILE, userId));

        if (recruiter.getVerificationStatus() != VerificationStatus.VERIFIED) {
            throw new ConflictException("Recruiter must be verified before posting jobs");
        }
        if (recruiter.getCompany() == null) {
            throw new ConflictException("Recruiter must be associated with a company before posting jobs");
        }

        Job job = Job.builder()
                .recruiter(recruiter)
                .company(recruiter.getCompany())
                .title(req.getTitle())
                .description(req.getDescription())
                .locationType(req.getLocationType())
                .jobType(req.getJobType())
                .locationCity(req.getLocationCity())
                .compensation(req.getCompensation())
                .applicationDeadline(req.getApplicationDeadline())
                .minCgpa(req.getMinCgpa())
                .status(JobStatus.PENDING_APPROVAL)
                .build();

        req.getRequiredSkills().forEach(skill ->
                job.getRequiredSkills().add(
                        JobRequiredSkill.builder().job(job).skillName(skill).build()));

        req.getEligibleDepartments().forEach(dept ->
                job.getEligibleDepartments().add(
                        JobEligibleDepartment.builder().job(job).departmentName(dept).build()));

        return jobMapper.toResponse(jobRepository.save(job));
    }

    @Caching(evict = {
            @CacheEvict(value = "job-listings", allEntries = true),
            @CacheEvict(value = "job-detail", key = "#jobId")
    })
    @Transactional
    public JobResponse updateJob(UUID userId, UUID jobId, UpdateJobRequest req) {
        log.info("Updating jobId={} by userId={}", jobId, userId);
        Job job = jobRepository.findByIdAndDeletedAtIsNull(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", jobId));

        RecruiterProfile recruiter = recruiterProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(RECRUITER_PROFILE, userId));

        if (!job.getRecruiter().getId().equals(recruiter.getId())) {
            throw new AccessDeniedException("Only the job poster can update it");
        }
        if (job.getStatus() != JobStatus.DRAFT && job.getStatus() != JobStatus.PENDING_APPROVAL) {
            throw new ConflictException("Job can only be updated in DRAFT or PENDING_APPROVAL status");
        }

        job.setTitle(req.getTitle());
        job.setDescription(req.getDescription());
        job.setLocationType(req.getLocationType());
        job.setJobType(req.getJobType());
        job.setLocationCity(req.getLocationCity());
        job.setCompensation(req.getCompensation());
        job.setApplicationDeadline(req.getApplicationDeadline());
        job.setMinCgpa(req.getMinCgpa());

        job.getRequiredSkills().clear();
        req.getRequiredSkills().forEach(skill ->
                job.getRequiredSkills().add(
                        JobRequiredSkill.builder().job(job).skillName(skill).build()));

        job.getEligibleDepartments().clear();
        req.getEligibleDepartments().forEach(dept ->
                job.getEligibleDepartments().add(
                        JobEligibleDepartment.builder().job(job).departmentName(dept).build()));

        return jobMapper.toResponse(jobRepository.save(job));
    }

    @Caching(evict = {
            @CacheEvict(value = "job-listings", allEntries = true),
            @CacheEvict(value = "job-detail", key = "#jobId")
    })
    @Transactional
    public void softDeleteJob(UUID userId, UUID jobId) {
        log.info("Soft-deleting jobId={} by userId={}", jobId, userId);
        Job job = jobRepository.findByIdAndDeletedAtIsNull(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", jobId));

        RecruiterProfile recruiter = recruiterProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(RECRUITER_PROFILE, userId));

        if (!job.getRecruiter().getId().equals(recruiter.getId())) {
            throw new AccessDeniedException("Only the job poster can delete it");
        }

        job.setDeletedAt(OffsetDateTime.now());
        jobRepository.save(job);
    }

    @Caching(evict = {
            @CacheEvict(value = "job-listings", allEntries = true),
            @CacheEvict(value = "job-detail", key = "#jobId")
    })
    @Transactional
    public JobResponse closeJob(UUID userId, UUID jobId) {
        log.info("Closing jobId={} by userId={}", jobId, userId);
        Job job = jobRepository.findByIdAndDeletedAtIsNull(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", jobId));

        RecruiterProfile recruiter = recruiterProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(RECRUITER_PROFILE, userId));

        if (!job.getRecruiter().getId().equals(recruiter.getId())) {
            throw new AccessDeniedException("Only the job poster can close it");
        }
        if (job.getStatus() != JobStatus.OPEN) {
            throw new ConflictException("Only OPEN jobs can be closed");
        }

        job.setStatus(JobStatus.CLOSED);
        job.setClosedAt(OffsetDateTime.now());
        return jobMapper.toResponse(jobRepository.save(job));
    }

    @Caching(evict = {
            @CacheEvict(value = "job-listings", allEntries = true),
            @CacheEvict(value = "job-detail", key = "#jobId")
    })
    @Transactional
    public JobResponse processApproval(UUID adminUserId, UUID jobId, JobApprovalRequest req) {
        log.info("Processing job approval: jobId={}, decision={}, adminUserId={}",
                jobId, req.getDecision(), adminUserId);
        Job job = jobRepository.findByIdAndDeletedAtIsNull(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", jobId));

        if (job.getStatus() != JobStatus.PENDING_APPROVAL) {
            throw new ConflictException("Job is not in PENDING_APPROVAL status");
        }

        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", adminUserId));

        if (req.getDecision() == JobApprovalRequest.Decision.APPROVE) {
            job.setStatus(JobStatus.OPEN);
            job.setApprovedBy(admin);
            job.setApprovedAt(OffsetDateTime.now());
        } else {
            if (req.getRejectionReason() == null || req.getRejectionReason().isBlank()) {
                throw new IllegalArgumentException("rejectionReason is required when rejecting a job");
            }
            job.setStatus(JobStatus.REJECTED);
            job.setApprovedBy(admin);
            job.setApprovedAt(OffsetDateTime.now());
        }

        return jobMapper.toResponse(jobRepository.save(job));
    }
}
