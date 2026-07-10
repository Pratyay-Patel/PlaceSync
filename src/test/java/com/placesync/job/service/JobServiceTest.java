package com.placesync.job.service;

import com.placesync.common.exception.ConflictException;
import com.placesync.common.exception.ResourceNotFoundException;
import com.placesync.job.dto.*;
import com.placesync.job.entity.Job;
import com.placesync.job.entity.JobLocationType;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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

        CreateJobRequest jobReq = createJobRequest();
        assertThatThrownBy(() -> jobService.createJob(userId, jobReq))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("verified");
    }

    @Test
    void createJob_noCompany_throwsConflictException() {
        RecruiterProfile recruiter = new RecruiterProfile();
        recruiter.setVerificationStatus(VerificationStatus.VERIFIED);
        recruiter.setCompany(null);
        when(recruiterProfileRepository.findByUserId(userId)).thenReturn(Optional.of(recruiter));

        CreateJobRequest jobReq2 = createJobRequest();
        assertThatThrownBy(() -> jobService.createJob(userId, jobReq2))
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

        UpdateJobRequest updateReq = new UpdateJobRequest();
        assertThatThrownBy(() -> jobService.updateJob(userId, jobId, updateReq))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void updateJob_openStatus_throwsConflictException() {
        RecruiterProfile recruiter = verifiedRecruiter();
        Job job = openJob(recruiter);
        job.setStatus(JobStatus.OPEN);
        when(jobRepository.findByIdAndDeletedAtIsNull(jobId)).thenReturn(Optional.of(job));
        when(recruiterProfileRepository.findByUserId(userId)).thenReturn(Optional.of(recruiter));

        UpdateJobRequest updateReq2 = new UpdateJobRequest();
        assertThatThrownBy(() -> jobService.updateJob(userId, jobId, updateReq2))
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
    void getJob_found_returnsJobResponse() {
        Job job = new Job();
        when(jobRepository.findByIdAndDeletedAtIsNull(jobId)).thenReturn(Optional.of(job));
        when(jobMapper.toResponse(job)).thenReturn(new JobResponse());

        JobResponse result = jobService.getJob(jobId);

        assertThat(result).isNotNull();
    }

    @Test
    void getJob_notFound_throwsResourceNotFoundException() {
        when(jobRepository.findByIdAndDeletedAtIsNull(jobId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.getJob(jobId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getOpenJobs_returnsPagedResponse() {
        when(jobRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        jobService.getOpenJobs(new JobFilterRequest(), Pageable.unpaged());

        verify(jobRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void getRecruiterJobs_returnsPagedResponse() {
        RecruiterProfile recruiter = verifiedRecruiter();
        when(recruiterProfileRepository.findByUserId(userId)).thenReturn(Optional.of(recruiter));
        when(jobRepository.findByRecruiterIdAndDeletedAtIsNull(recruiter.getId(), Pageable.unpaged()))
                .thenReturn(new PageImpl<>(List.of()));

        jobService.getRecruiterJobs(userId, Pageable.unpaged());

        verify(jobRepository).findByRecruiterIdAndDeletedAtIsNull(recruiter.getId(), Pageable.unpaged());
    }

    @Test
    void getPendingJobs_returnsPagedResponse() {
        when(jobRepository.findByStatusAndDeletedAtIsNull(JobStatus.PENDING_APPROVAL, Pageable.unpaged()))
                .thenReturn(new PageImpl<>(List.of()));

        jobService.getPendingJobs(Pageable.unpaged());

        verify(jobRepository).findByStatusAndDeletedAtIsNull(JobStatus.PENDING_APPROVAL, Pageable.unpaged());
    }

    @Test
    void softDeleteJob_byOwner_setsDeletedAt() {
        RecruiterProfile recruiter = verifiedRecruiter();
        Job job = openJob(recruiter);
        when(jobRepository.findByIdAndDeletedAtIsNull(jobId)).thenReturn(Optional.of(job));
        when(recruiterProfileRepository.findByUserId(userId)).thenReturn(Optional.of(recruiter));

        jobService.softDeleteJob(userId, jobId);

        assertThat(job.getDeletedAt()).isNotNull();
        verify(jobRepository).save(job);
    }

    @Test
    void softDeleteJob_byOtherRecruiter_throwsAccessDeniedException() {
        RecruiterProfile owner = verifiedRecruiter();
        RecruiterProfile other = verifiedRecruiter();
        Job job = openJob(owner);
        when(jobRepository.findByIdAndDeletedAtIsNull(jobId)).thenReturn(Optional.of(job));
        when(recruiterProfileRepository.findByUserId(userId)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> jobService.softDeleteJob(userId, jobId))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ── ONSITE city validation — createJob ───────────────────────────────────

    @Test
    void createJob_onsiteWithNullCity_throwsIllegalArgumentException() {
        RecruiterProfile recruiter = verifiedRecruiter();
        when(recruiterProfileRepository.findByUserId(userId)).thenReturn(Optional.of(recruiter));
        CreateJobRequest req = createJobRequest();
        req.setLocationType(JobLocationType.ONSITE);
        req.setLocationCity(null);

        assertThatThrownBy(() -> jobService.createJob(userId, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("City is required for on-site jobs");
    }

    @Test
    void createJob_onsiteWithBlankCity_throwsIllegalArgumentException() {
        RecruiterProfile recruiter = verifiedRecruiter();
        when(recruiterProfileRepository.findByUserId(userId)).thenReturn(Optional.of(recruiter));
        CreateJobRequest req = createJobRequest();
        req.setLocationType(JobLocationType.ONSITE);
        req.setLocationCity("   ");

        assertThatThrownBy(() -> jobService.createJob(userId, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("City is required for on-site jobs");
    }

    @Test
    void createJob_onsiteWithValidCity_savesJob() {
        RecruiterProfile recruiter = verifiedRecruiter();
        when(recruiterProfileRepository.findByUserId(userId)).thenReturn(Optional.of(recruiter));
        when(jobRepository.save(any())).thenReturn(new Job());
        when(jobMapper.toResponse(any())).thenReturn(new JobResponse());
        CreateJobRequest req = createJobRequest();
        req.setLocationType(JobLocationType.ONSITE);
        req.setLocationCity("Mumbai");

        jobService.createJob(userId, req);

        verify(jobRepository).save(any(Job.class));
    }

    // ── ONSITE city validation — updateJob ───────────────────────────────────

    private Job pendingJob(RecruiterProfile recruiter) {
        Job j = openJob(recruiter);
        j.setStatus(JobStatus.PENDING_APPROVAL);
        return j;
    }

    @Test
    void updateJob_onsiteWithNullCity_throwsIllegalArgumentException() {
        RecruiterProfile recruiter = verifiedRecruiter();
        Job job = pendingJob(recruiter);
        when(jobRepository.findByIdAndDeletedAtIsNull(jobId)).thenReturn(Optional.of(job));
        when(recruiterProfileRepository.findByUserId(userId)).thenReturn(Optional.of(recruiter));
        UpdateJobRequest req = new UpdateJobRequest();
        req.setTitle("T");
        req.setLocationType(JobLocationType.ONSITE);
        req.setLocationCity(null);
        req.setRequiredSkills(List.of());
        req.setEligibleDepartments(List.of());

        assertThatThrownBy(() -> jobService.updateJob(userId, jobId, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("City is required for on-site jobs");
    }

    @Test
    void updateJob_onsiteWithBlankCity_throwsIllegalArgumentException() {
        RecruiterProfile recruiter = verifiedRecruiter();
        Job job = pendingJob(recruiter);
        when(jobRepository.findByIdAndDeletedAtIsNull(jobId)).thenReturn(Optional.of(job));
        when(recruiterProfileRepository.findByUserId(userId)).thenReturn(Optional.of(recruiter));
        UpdateJobRequest req = new UpdateJobRequest();
        req.setTitle("T");
        req.setLocationType(JobLocationType.ONSITE);
        req.setLocationCity("   ");
        req.setRequiredSkills(List.of());
        req.setEligibleDepartments(List.of());

        assertThatThrownBy(() -> jobService.updateJob(userId, jobId, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("City is required for on-site jobs");
    }

    @Test
    void updateJob_onsiteWithValidCity_updatesJob() {
        RecruiterProfile recruiter = verifiedRecruiter();
        Job job = pendingJob(recruiter);
        when(jobRepository.findByIdAndDeletedAtIsNull(jobId)).thenReturn(Optional.of(job));
        when(recruiterProfileRepository.findByUserId(userId)).thenReturn(Optional.of(recruiter));
        when(jobRepository.save(job)).thenReturn(job);
        when(jobMapper.toResponse(job)).thenReturn(new JobResponse());
        UpdateJobRequest req = new UpdateJobRequest();
        req.setTitle("T");
        req.setLocationType(JobLocationType.ONSITE);
        req.setLocationCity("Bangalore");
        req.setRequiredSkills(List.of());
        req.setEligibleDepartments(List.of());

        jobService.updateJob(userId, jobId, req);

        assertThat(job.getLocationCity()).isEqualTo("Bangalore");
    }
}
