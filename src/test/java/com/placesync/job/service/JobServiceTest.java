package com.placesync.job.service;

import com.placesync.common.exception.ConflictException;
import com.placesync.common.exception.ResourceNotFoundException;
import com.placesync.job.dto.*;
import com.placesync.job.entity.Job;
import com.placesync.job.entity.JobStatus;
import com.placesync.job.mapper.JobMapper;
import com.placesync.job.repository.JobRepository;
import com.placesync.recruiter.entity.RecruiterProfile;
import com.placesync.recruiter.entity.VerificationStatus;
import com.placesync.recruiter.repository.RecruiterProfileRepository;
import com.placesync.user.entity.User;
import com.placesync.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock JobRepository jobRepository;
    @Mock JobMapper jobMapper;
    @Mock RecruiterProfileRepository recruiterProfileRepository;
    @Mock UserRepository userRepository;

    @InjectMocks JobService jobService;

    private final UUID userId = UUID.randomUUID();
    private final UUID jobId = UUID.randomUUID();

    private RecruiterProfile verifiedRecruiter() {
        RecruiterProfile r = new RecruiterProfile();
        r.setId(UUID.randomUUID());
        r.setVerificationStatus(VerificationStatus.VERIFIED);
        r.setCompany(new com.placesync.company.entity.Company());
        return r;
    }

    private Job openJob(RecruiterProfile recruiter) {
        Job j = new Job();
        j.setId(jobId);
        j.setRecruiter(recruiter);
        j.setStatus(JobStatus.OPEN);
        j.setRequiredSkills(new ArrayList<>());
        j.setEligibleDepartments(new ArrayList<>());
        return j;
    }

    private CreateJobRequest createJobRequest() {
        CreateJobRequest req = new CreateJobRequest();
        req.setTitle("Dev");
        req.setDescription("Desc");
        req.setRequiredSkills(List.of());
        req.setEligibleDepartments(List.of());
        return req;
    }

    @Test
    void createJob_verifiedRecruiterWithCompany_savesJob() {
        RecruiterProfile recruiter = verifiedRecruiter();
        when(recruiterProfileRepository.findByUserId(userId)).thenReturn(Optional.of(recruiter));
        when(jobRepository.save(any())).thenReturn(new Job());
        when(jobMapper.toResponse(any())).thenReturn(new JobResponse());

        jobService.createJob(userId, createJobRequest());

        verify(jobRepository).save(any(Job.class));
    }

    @Test
    void createJob_unverifiedRecruiter_throwsConflictException() {
        RecruiterProfile recruiter = new RecruiterProfile();
        recruiter.setVerificationStatus(VerificationStatus.PENDING_VERIFICATION);
        when(recruiterProfileRepository.findByUserId(userId)).thenReturn(Optional.of(recruiter));

        assertThatThrownBy(() -> jobService.createJob(userId, createJobRequest()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("verified");
    }

    @Test
    void createJob_noCompany_throwsConflictException() {
        RecruiterProfile recruiter = new RecruiterProfile();
        recruiter.setVerificationStatus(VerificationStatus.VERIFIED);
        recruiter.setCompany(null);
        when(recruiterProfileRepository.findByUserId(userId)).thenReturn(Optional.of(recruiter));

        assertThatThrownBy(() -> jobService.createJob(userId, createJobRequest()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("company");
    }

    @Test
    void updateJob_byOwner_inPendingStatus_updatesJob() {
        RecruiterProfile recruiter = verifiedRecruiter();
        Job job = openJob(recruiter);
        job.setStatus(JobStatus.PENDING_APPROVAL);
        UpdateJobRequest req = new UpdateJobRequest();
        req.setTitle("Updated");
        req.setRequiredSkills(List.of());
        req.setEligibleDepartments(List.of());
        when(jobRepository.findByIdAndDeletedAtIsNull(jobId)).thenReturn(Optional.of(job));
        when(recruiterProfileRepository.findByUserId(userId)).thenReturn(Optional.of(recruiter));
        when(jobRepository.save(job)).thenReturn(job);
        when(jobMapper.toResponse(job)).thenReturn(new JobResponse());

        jobService.updateJob(userId, jobId, req);

        assertThat(job.getTitle()).isEqualTo("Updated");
    }

    @Test
    void updateJob_byOtherRecruiter_throwsAccessDeniedException() {
        RecruiterProfile owner = verifiedRecruiter();
        RecruiterProfile other = verifiedRecruiter();
        Job job = openJob(owner);
        job.setStatus(JobStatus.PENDING_APPROVAL);
        when(jobRepository.findByIdAndDeletedAtIsNull(jobId)).thenReturn(Optional.of(job));
        when(recruiterProfileRepository.findByUserId(userId)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> jobService.updateJob(userId, jobId, new UpdateJobRequest()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void updateJob_openStatus_throwsConflictException() {
        RecruiterProfile recruiter = verifiedRecruiter();
        Job job = openJob(recruiter);
        job.setStatus(JobStatus.OPEN);
        when(jobRepository.findByIdAndDeletedAtIsNull(jobId)).thenReturn(Optional.of(job));
        when(recruiterProfileRepository.findByUserId(userId)).thenReturn(Optional.of(recruiter));

        assertThatThrownBy(() -> jobService.updateJob(userId, jobId, new UpdateJobRequest()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void processApproval_approve_setsOpenStatus() {
        Job job = new Job();
        job.setId(jobId);
        job.setStatus(JobStatus.PENDING_APPROVAL);
        job.setRequiredSkills(new ArrayList<>());
        job.setEligibleDepartments(new ArrayList<>());
        User admin = new User();
        JobApprovalRequest req = new JobApprovalRequest();
        req.setDecision(JobApprovalRequest.Decision.APPROVE);
        when(jobRepository.findByIdAndDeletedAtIsNull(jobId)).thenReturn(Optional.of(job));
        when(userRepository.findById(userId)).thenReturn(Optional.of(admin));
        when(jobRepository.save(job)).thenReturn(job);
        when(jobMapper.toResponse(job)).thenReturn(new JobResponse());

        jobService.processApproval(userId, jobId, req);

        assertThat(job.getStatus()).isEqualTo(JobStatus.OPEN);
    }

    @Test
    void processApproval_reject_requiresReason() {
        Job job = new Job();
        job.setId(jobId);
        job.setStatus(JobStatus.PENDING_APPROVAL);
        User admin = new User();
        JobApprovalRequest req = new JobApprovalRequest();
        req.setDecision(JobApprovalRequest.Decision.REJECT);
        when(jobRepository.findByIdAndDeletedAtIsNull(jobId)).thenReturn(Optional.of(job));
        when(userRepository.findById(userId)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> jobService.processApproval(userId, jobId, req))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void closeJob_openJob_setsClosedStatus() {
        RecruiterProfile recruiter = verifiedRecruiter();
        Job job = openJob(recruiter);
        when(jobRepository.findByIdAndDeletedAtIsNull(jobId)).thenReturn(Optional.of(job));
        when(recruiterProfileRepository.findByUserId(userId)).thenReturn(Optional.of(recruiter));
        when(jobRepository.save(job)).thenReturn(job);
        when(jobMapper.toResponse(job)).thenReturn(new JobResponse());

        jobService.closeJob(userId, jobId);

        assertThat(job.getStatus()).isEqualTo(JobStatus.CLOSED);
        assertThat(job.getClosedAt()).isNotNull();
    }

    @Test
    void closeJob_nonOpenJob_throwsConflictException() {
        RecruiterProfile recruiter = verifiedRecruiter();
        Job job = openJob(recruiter);
        job.setStatus(JobStatus.CLOSED);
        when(jobRepository.findByIdAndDeletedAtIsNull(jobId)).thenReturn(Optional.of(job));
        when(recruiterProfileRepository.findByUserId(userId)).thenReturn(Optional.of(recruiter));

        assertThatThrownBy(() -> jobService.closeJob(userId, jobId))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void getJob_notFound_throwsResourceNotFoundException() {
        when(jobRepository.findByIdAndDeletedAtIsNull(jobId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.getJob(jobId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
