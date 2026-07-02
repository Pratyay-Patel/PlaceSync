package com.placesync.application.service;

import com.placesync.application.dto.ApplyRequest;
import com.placesync.application.dto.ApplicationResponse;
import com.placesync.application.dto.UpdateApplicationStatusRequest;
import com.placesync.application.entity.Application;
import com.placesync.application.entity.ApplicationStatus;
import com.placesync.application.mapper.ApplicationMapper;
import com.placesync.application.repository.ApplicationRepository;
import com.placesync.common.exception.ConflictException;
import com.placesync.common.exception.ResourceNotFoundException;
import com.placesync.common.kafka.KafkaEventPublisher;
import com.placesync.company.entity.Company;
import com.placesync.job.entity.Job;
import com.placesync.job.entity.JobStatus;
import com.placesync.job.repository.JobRepository;
import com.placesync.recruiter.entity.RecruiterProfile;
import com.placesync.recruiter.repository.RecruiterProfileRepository;
import com.placesync.user.entity.Resume;
import com.placesync.user.entity.StudentProfile;
import com.placesync.user.entity.User;
import com.placesync.user.repository.ResumeRepository;
import com.placesync.user.repository.StudentProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock ApplicationRepository applicationRepository;
    @Mock ApplicationMapper applicationMapper;
    @Mock StudentProfileRepository studentProfileRepository;
    @Mock JobRepository jobRepository;
    @Mock ResumeRepository resumeRepository;
    @Mock RecruiterProfileRepository recruiterProfileRepository;
    @Mock KafkaEventPublisher kafkaEventPublisher;

    @InjectMocks ApplicationService applicationService;

    private final UUID userId = UUID.randomUUID();
    private final UUID jobId = UUID.randomUUID();
    private final UUID resumeId = UUID.randomUUID();
    private final UUID applicationId = UUID.randomUUID();

    private StudentProfile studentProfile() {
        User user = new User();
        user.setId(userId);
        user.setEmail("student@test.com");
        StudentProfile sp = new StudentProfile();
        sp.setId(UUID.randomUUID());
        sp.setUser(user);
        sp.setDepartment("CS");
        return sp;
    }

    private Job openJob() {
        Company company = new Company();
        company.setName("Acme");
        Job job = new Job();
        job.setId(jobId);
        job.setTitle("Dev");
        job.setStatus(JobStatus.OPEN);
        job.setCompany(company);
        job.setEligibleDepartments(new ArrayList<>());
        return job;
    }

    private Resume ownedResume(StudentProfile student) {
        Resume r = new Resume();
        r.setId(resumeId);
        r.setStudent(student);
        return r;
    }

    private void mockApplyHappyPath(StudentProfile student, Job job, Resume resume) {
        when(studentProfileRepository.findByUserId(userId)).thenReturn(Optional.of(student));
        when(jobRepository.findByIdAndDeletedAtIsNull(jobId)).thenReturn(Optional.of(job));
        when(resumeRepository.findById(resumeId)).thenReturn(Optional.of(resume));
        when(applicationRepository.existsByStudentIdAndJobId(student.getId(), jobId)).thenReturn(false);
        Application saved = new Application();
        saved.setId(applicationId);
        saved.setStudent(student);
        saved.setJob(job);
        when(applicationRepository.save(any())).thenReturn(saved);
        when(applicationMapper.toResponse(saved)).thenReturn(new ApplicationResponse());
    }

    @Test
    void apply_validRequest_savesApplicationAndPublishesEvent() {
        StudentProfile student = studentProfile();
        Job job = openJob();
        Resume resume = ownedResume(student);
        ApplyRequest req = new ApplyRequest();
        req.setJobId(jobId);
        req.setResumeId(resumeId);
        mockApplyHappyPath(student, job, resume);

        applicationService.apply(userId, req);

        verify(applicationRepository).save(any(Application.class));
        verify(kafkaEventPublisher).publish(any());
    }

    @Test
    void apply_duplicateApplication_throwsConflictException() {
        StudentProfile student = studentProfile();
        Job job = openJob();
        Resume resume = ownedResume(student);
        ApplyRequest req = new ApplyRequest();
        req.setJobId(jobId);
        req.setResumeId(resumeId);
        when(studentProfileRepository.findByUserId(userId)).thenReturn(Optional.of(student));
        when(jobRepository.findByIdAndDeletedAtIsNull(jobId)).thenReturn(Optional.of(job));
        when(resumeRepository.findById(resumeId)).thenReturn(Optional.of(resume));
        when(applicationRepository.existsByStudentIdAndJobId(student.getId(), jobId)).thenReturn(true);

        assertThatThrownBy(() -> applicationService.apply(userId, req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already applied");
    }

    @Test
    void apply_jobNotOpen_throwsConflictException() {
        StudentProfile student = studentProfile();
        Job job = openJob();
        job.setStatus(JobStatus.CLOSED);
        Resume resume = ownedResume(student);
        ApplyRequest req = new ApplyRequest();
        req.setJobId(jobId);
        req.setResumeId(resumeId);
        when(studentProfileRepository.findByUserId(userId)).thenReturn(Optional.of(student));
        when(jobRepository.findByIdAndDeletedAtIsNull(jobId)).thenReturn(Optional.of(job));
        when(resumeRepository.findById(resumeId)).thenReturn(Optional.of(resume));

        assertThatThrownBy(() -> applicationService.apply(userId, req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("not currently accepting");
    }

    @Test
    void apply_resumeNotOwnedByStudent_throwsAccessDeniedException() {
        StudentProfile student = studentProfile();
        StudentProfile other = studentProfile();
        Job job = openJob();
        Resume resume = ownedResume(other);
        ApplyRequest req = new ApplyRequest();
        req.setJobId(jobId);
        req.setResumeId(resumeId);
        when(studentProfileRepository.findByUserId(userId)).thenReturn(Optional.of(student));
        when(jobRepository.findByIdAndDeletedAtIsNull(jobId)).thenReturn(Optional.of(job));
        when(resumeRepository.findById(resumeId)).thenReturn(Optional.of(resume));

        assertThatThrownBy(() -> applicationService.apply(userId, req))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void apply_cgpaBelowMinimum_throwsConflictException() {
        StudentProfile student = studentProfile();
        student.setCgpa(new BigDecimal("6.0"));
        Job job = openJob();
        job.setMinCgpa(new BigDecimal("7.5"));
        Resume resume = ownedResume(student);
        ApplyRequest req = new ApplyRequest();
        req.setJobId(jobId);
        req.setResumeId(resumeId);
        when(studentProfileRepository.findByUserId(userId)).thenReturn(Optional.of(student));
        when(jobRepository.findByIdAndDeletedAtIsNull(jobId)).thenReturn(Optional.of(job));
        when(resumeRepository.findById(resumeId)).thenReturn(Optional.of(resume));
        when(applicationRepository.existsByStudentIdAndJobId(student.getId(), jobId)).thenReturn(false);

        assertThatThrownBy(() -> applicationService.apply(userId, req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("CGPA");
    }

    @Test
    void apply_studentNotFound_throwsResourceNotFoundException() {
        when(studentProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        ApplyRequest applyReq = new ApplyRequest();
        assertThatThrownBy(() -> applicationService.apply(userId, applyReq))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateStatus_validTransition_savesAndPublishesEvent() {
        RecruiterProfile recruiter = new RecruiterProfile();
        recruiter.setId(UUID.randomUUID());
        Job job = openJob();
        job.setRecruiter(recruiter);
        User studentUser = new User();
        studentUser.setId(UUID.randomUUID());
        StudentProfile student = studentProfile();
        student.setUser(studentUser);
        Application application = new Application();
        application.setId(applicationId);
        application.setStatus(ApplicationStatus.APPLIED);
        application.setJob(job);
        application.setStudent(student);
        UpdateApplicationStatusRequest req = new UpdateApplicationStatusRequest();
        req.setStatus(ApplicationStatus.UNDER_REVIEW);
        when(recruiterProfileRepository.findByUserId(userId)).thenReturn(Optional.of(recruiter));
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(applicationRepository.save(application)).thenReturn(application);
        when(applicationMapper.toResponse(application)).thenReturn(new ApplicationResponse());

        applicationService.updateStatus(userId, applicationId, req);

        verify(kafkaEventPublisher).publish(any());
    }

    @Test
    void updateStatus_invalidTransition_throwsIllegalArgumentException() {
        RecruiterProfile recruiter = new RecruiterProfile();
        recruiter.setId(UUID.randomUUID());
        Job job = openJob();
        job.setRecruiter(recruiter);
        Application application = new Application();
        application.setStatus(ApplicationStatus.REJECTED);
        application.setJob(job);
        UpdateApplicationStatusRequest req = new UpdateApplicationStatusRequest();
        req.setStatus(ApplicationStatus.OFFERED);
        when(recruiterProfileRepository.findByUserId(userId)).thenReturn(Optional.of(recruiter));
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> applicationService.updateStatus(userId, applicationId, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid status transition");
    }

    @Test
    void updateStatus_byNonOwnerRecruiter_throwsAccessDeniedException() {
        RecruiterProfile recruiter = new RecruiterProfile();
        recruiter.setId(UUID.randomUUID());
        RecruiterProfile owner = new RecruiterProfile();
        owner.setId(UUID.randomUUID());
        Job job = openJob();
        job.setRecruiter(owner);
        Application application = new Application();
        application.setStatus(ApplicationStatus.APPLIED);
        application.setJob(job);
        UpdateApplicationStatusRequest req = new UpdateApplicationStatusRequest();
        req.setStatus(ApplicationStatus.UNDER_REVIEW);
        when(recruiterProfileRepository.findByUserId(userId)).thenReturn(Optional.of(recruiter));
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> applicationService.updateStatus(userId, applicationId, req))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getMyApplications_returnsPagedResponse() {
        StudentProfile student = studentProfile();
        when(studentProfileRepository.findByUserId(userId)).thenReturn(Optional.of(student));
        when(applicationRepository.findByStudentId(eq(student.getId()), any())).thenReturn(new PageImpl<>(List.of()));

        applicationService.getMyApplications(userId, Pageable.unpaged());

        verify(applicationRepository).findByStudentId(eq(student.getId()), any());
    }

    @Test
    void getMyApplication_ownedByStudent_returnsResponse() {
        StudentProfile student = studentProfile();
        Application application = new Application();
        application.setId(applicationId);
        application.setStudent(student);
        when(studentProfileRepository.findByUserId(userId)).thenReturn(Optional.of(student));
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(applicationMapper.toResponse(application)).thenReturn(new ApplicationResponse());

        ApplicationResponse result = applicationService.getMyApplication(userId, applicationId);

        assertThat(result).isNotNull();
    }

    @Test
    void getMyApplication_notOwnedByStudent_throwsAccessDeniedException() {
        StudentProfile student = studentProfile();
        StudentProfile other = studentProfile();
        Application application = new Application();
        application.setId(applicationId);
        application.setStudent(other);
        when(studentProfileRepository.findByUserId(userId)).thenReturn(Optional.of(student));
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> applicationService.getMyApplication(userId, applicationId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getJobApplications_byJobOwner_returnsPagedResponse() {
        RecruiterProfile recruiter = new RecruiterProfile();
        recruiter.setId(UUID.randomUUID());
        Job job = openJob();
        job.setRecruiter(recruiter);
        when(recruiterProfileRepository.findByUserId(userId)).thenReturn(Optional.of(recruiter));
        when(jobRepository.findByIdAndDeletedAtIsNull(jobId)).thenReturn(Optional.of(job));
        when(applicationRepository.findByJobId(eq(jobId), any())).thenReturn(new PageImpl<>(List.of()));

        applicationService.getJobApplications(userId, jobId, Pageable.unpaged());

        verify(applicationRepository).findByJobId(eq(jobId), any());
    }

    @Test
    void getJobApplications_byNonOwnerRecruiter_throwsAccessDeniedException() {
        RecruiterProfile recruiter = new RecruiterProfile();
        recruiter.setId(UUID.randomUUID());
        RecruiterProfile owner = new RecruiterProfile();
        owner.setId(UUID.randomUUID());
        Job job = openJob();
        job.setRecruiter(owner);
        when(recruiterProfileRepository.findByUserId(userId)).thenReturn(Optional.of(recruiter));
        when(jobRepository.findByIdAndDeletedAtIsNull(jobId)).thenReturn(Optional.of(job));

        Pageable pageable = Pageable.unpaged();
        assertThatThrownBy(() -> applicationService.getJobApplications(userId, jobId, pageable))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getAllApplicationsForAdmin_returnsPagedResponse() {
        when(applicationRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        applicationService.getAllApplicationsForAdmin(null, Pageable.unpaged());

        verify(applicationRepository).findAll(any(Specification.class), any(Pageable.class));
    }
}
