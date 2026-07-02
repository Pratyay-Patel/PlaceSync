package com.placesync.interview.service;

import com.placesync.application.entity.Application;
import com.placesync.application.entity.ApplicationStatus;
import com.placesync.application.repository.ApplicationRepository;
import com.placesync.common.exception.ConflictException;
import com.placesync.common.exception.ResourceNotFoundException;
import com.placesync.common.kafka.KafkaEventPublisher;
import com.placesync.interview.dto.*;
import com.placesync.interview.entity.Interview;
import com.placesync.interview.entity.InterviewStatus;
import com.placesync.interview.mapper.InterviewMapper;
import com.placesync.interview.repository.InterviewRepository;
import com.placesync.job.entity.Job;
import com.placesync.recruiter.entity.RecruiterProfile;
import com.placesync.recruiter.repository.RecruiterProfileRepository;
import com.placesync.user.entity.StudentProfile;
import com.placesync.user.entity.User;
import com.placesync.user.repository.StudentProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InterviewServiceTest {

    @Mock InterviewRepository interviewRepository;
    @Mock InterviewMapper interviewMapper;
    @Mock ApplicationRepository applicationRepository;
    @Mock StudentProfileRepository studentProfileRepository;
    @Mock RecruiterProfileRepository recruiterProfileRepository;
    @Mock KafkaEventPublisher kafkaEventPublisher;

    @InjectMocks InterviewService interviewService;

    private final UUID userId = UUID.randomUUID();
    private final UUID applicationId = UUID.randomUUID();
    private final UUID interviewId = UUID.randomUUID();

    private RecruiterProfile recruiter() {
        RecruiterProfile r = new RecruiterProfile();
        r.setId(UUID.randomUUID());
        return r;
    }

    private Application applicationFor(RecruiterProfile recruiter) {
        User studentUser = new User();
        studentUser.setId(UUID.randomUUID());
        StudentProfile student = new StudentProfile();
        student.setUser(studentUser);
        Job job = new Job();
        job.setRecruiter(recruiter);
        Application app = new Application();
        app.setId(applicationId);
        app.setJob(job);
        app.setStudent(student);
        app.setStatus(ApplicationStatus.SHORTLISTED);
        return app;
    }

    private Interview scheduledInterview(Application application) {
        Interview i = new Interview();
        i.setId(interviewId);
        i.setApplication(application);
        i.setStatus(InterviewStatus.SCHEDULED);
        i.setScheduledAt(OffsetDateTime.now().plusDays(1));
        return i;
    }

    @Test
    void scheduleInterview_validRequest_savesAndPublishesEvent() {
        RecruiterProfile recruiter = recruiter();
        Application application = applicationFor(recruiter);
        ScheduleInterviewRequest req = new ScheduleInterviewRequest();
        req.setRoundNumber((short) 1);
        req.setScheduledAt(OffsetDateTime.now().plusDays(1));
        when(recruiterProfileRepository.findByUserId(userId)).thenReturn(Optional.of(recruiter));
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(interviewRepository.existsByApplicationIdAndRoundNumber(applicationId, (short) 1)).thenReturn(false);
        Interview saved = scheduledInterview(application);
        when(interviewRepository.save(any())).thenReturn(saved);
        when(applicationRepository.save(application)).thenReturn(application);
        when(interviewMapper.toResponse(saved)).thenReturn(new InterviewResponse());

        interviewService.scheduleInterview(userId, applicationId, req);

        verify(kafkaEventPublisher).publish(any());
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.INTERVIEW_SCHEDULED);
    }

    @Test
    void scheduleInterview_duplicateRound_throwsConflictException() {
        RecruiterProfile recruiter = recruiter();
        Application application = applicationFor(recruiter);
        ScheduleInterviewRequest req = new ScheduleInterviewRequest();
        req.setRoundNumber((short) 1);
        req.setScheduledAt(OffsetDateTime.now().plusDays(1));
        when(recruiterProfileRepository.findByUserId(userId)).thenReturn(Optional.of(recruiter));
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(interviewRepository.existsByApplicationIdAndRoundNumber(applicationId, (short) 1)).thenReturn(true);

        assertThatThrownBy(() -> interviewService.scheduleInterview(userId, applicationId, req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void scheduleInterview_byNonOwnerRecruiter_throwsAccessDeniedException() {
        RecruiterProfile owner = recruiter();
        RecruiterProfile other = recruiter();
        Application application = applicationFor(owner);
        when(recruiterProfileRepository.findByUserId(userId)).thenReturn(Optional.of(other));
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

        ScheduleInterviewRequest schedReq = new ScheduleInterviewRequest();
        assertThatThrownBy(() -> interviewService.scheduleInterview(userId, applicationId, schedReq))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void rescheduleInterview_scheduledInterview_updatesAndPublishesEvent() {
        RecruiterProfile recruiter = recruiter();
        Application application = applicationFor(recruiter);
        Interview interview = scheduledInterview(application);
        UpdateInterviewRequest req = new UpdateInterviewRequest();
        req.setScheduledAt(OffsetDateTime.now().plusDays(3));
        when(recruiterProfileRepository.findByUserId(userId)).thenReturn(Optional.of(recruiter));
        when(interviewRepository.findById(interviewId)).thenReturn(Optional.of(interview));
        when(interviewRepository.save(interview)).thenReturn(interview);
        when(interviewMapper.toResponse(interview)).thenReturn(new InterviewResponse());

        interviewService.rescheduleInterview(userId, interviewId, req);

        verify(kafkaEventPublisher).publish(any());
        assertThat(interview.getStatus()).isEqualTo(InterviewStatus.RESCHEDULED);
    }

    @Test
    void rescheduleInterview_cancelledInterview_throwsConflictException() {
        RecruiterProfile recruiter = recruiter();
        Application application = applicationFor(recruiter);
        Interview interview = scheduledInterview(application);
        interview.setStatus(InterviewStatus.CANCELLED);
        when(recruiterProfileRepository.findByUserId(userId)).thenReturn(Optional.of(recruiter));
        when(interviewRepository.findById(interviewId)).thenReturn(Optional.of(interview));

        UpdateInterviewRequest updateReq = new UpdateInterviewRequest();
        assertThatThrownBy(() -> interviewService.rescheduleInterview(userId, interviewId, updateReq))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void cancelInterview_scheduledInterview_setsCancelledStatus() {
        RecruiterProfile recruiter = recruiter();
        Application application = applicationFor(recruiter);
        Interview interview = scheduledInterview(application);
        CancelInterviewRequest req = new CancelInterviewRequest();
        req.setCancellationReason("Candidate withdrew");
        when(recruiterProfileRepository.findByUserId(userId)).thenReturn(Optional.of(recruiter));
        when(interviewRepository.findById(interviewId)).thenReturn(Optional.of(interview));
        when(interviewRepository.save(interview)).thenReturn(interview);
        when(interviewMapper.toResponse(interview)).thenReturn(new InterviewResponse());

        interviewService.cancelInterview(userId, interviewId, req);

        assertThat(interview.getStatus()).isEqualTo(InterviewStatus.CANCELLED);
        verify(kafkaEventPublisher).publish(any());
    }

    @Test
    void cancelInterview_alreadyCancelled_throwsConflictException() {
        RecruiterProfile recruiter = recruiter();
        Application application = applicationFor(recruiter);
        Interview interview = scheduledInterview(application);
        interview.setStatus(InterviewStatus.CANCELLED);
        when(recruiterProfileRepository.findByUserId(userId)).thenReturn(Optional.of(recruiter));
        when(interviewRepository.findById(interviewId)).thenReturn(Optional.of(interview));

        CancelInterviewRequest cancelReq = new CancelInterviewRequest();
        assertThatThrownBy(() -> interviewService.cancelInterview(userId, interviewId, cancelReq))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void cancelInterview_completedInterview_throwsConflictException() {
        RecruiterProfile recruiter = recruiter();
        Application application = applicationFor(recruiter);
        Interview interview = scheduledInterview(application);
        interview.setStatus(InterviewStatus.COMPLETED);
        when(recruiterProfileRepository.findByUserId(userId)).thenReturn(Optional.of(recruiter));
        when(interviewRepository.findById(interviewId)).thenReturn(Optional.of(interview));

        CancelInterviewRequest cancelReq2 = new CancelInterviewRequest();
        assertThatThrownBy(() -> interviewService.cancelInterview(userId, interviewId, cancelReq2))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void completeInterview_scheduledInterview_setsCompletedStatus() {
        RecruiterProfile recruiter = recruiter();
        Application application = applicationFor(recruiter);
        Interview interview = scheduledInterview(application);
        when(recruiterProfileRepository.findByUserId(userId)).thenReturn(Optional.of(recruiter));
        when(interviewRepository.findById(interviewId)).thenReturn(Optional.of(interview));
        when(interviewRepository.save(interview)).thenReturn(interview);
        when(interviewMapper.toResponse(interview)).thenReturn(new InterviewResponse());

        interviewService.completeInterview(userId, interviewId);

        assertThat(interview.getStatus()).isEqualTo(InterviewStatus.COMPLETED);
    }

    @Test
    void completeInterview_cancelledInterview_throwsConflictException() {
        RecruiterProfile recruiter = recruiter();
        Application application = applicationFor(recruiter);
        Interview interview = scheduledInterview(application);
        interview.setStatus(InterviewStatus.CANCELLED);
        when(recruiterProfileRepository.findByUserId(userId)).thenReturn(Optional.of(recruiter));
        when(interviewRepository.findById(interviewId)).thenReturn(Optional.of(interview));

        assertThatThrownBy(() -> interviewService.completeInterview(userId, interviewId))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void scheduleInterview_applicationNotFound_throwsResourceNotFoundException() {
        RecruiterProfile recruiter = recruiter();
        when(recruiterProfileRepository.findByUserId(userId)).thenReturn(Optional.of(recruiter));
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.empty());

        ScheduleInterviewRequest schedReq2 = new ScheduleInterviewRequest();
        assertThatThrownBy(() -> interviewService.scheduleInterview(userId, applicationId, schedReq2))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getMyInterviews_returnsNonCancelledInterviews() {
        StudentProfile student = new StudentProfile();
        student.setId(UUID.randomUUID());
        Interview scheduled = scheduledInterview(applicationFor(recruiter()));
        when(studentProfileRepository.findByUserId(userId)).thenReturn(Optional.of(student));
        when(interviewRepository.findByApplication_StudentIdOrderByScheduledAtAsc(student.getId()))
                .thenReturn(List.of(scheduled));
        when(interviewMapper.toResponse(scheduled)).thenReturn(new InterviewResponse());

        List<InterviewResponse> result = interviewService.getMyInterviews(userId);

        assertThat(result).hasSize(1);
    }

    @Test
    void getMyInterviews_filtersCancelledInterviews() {
        StudentProfile student = new StudentProfile();
        student.setId(UUID.randomUUID());
        Interview cancelled = scheduledInterview(applicationFor(recruiter()));
        cancelled.setStatus(InterviewStatus.CANCELLED);
        when(studentProfileRepository.findByUserId(userId)).thenReturn(Optional.of(student));
        when(interviewRepository.findByApplication_StudentIdOrderByScheduledAtAsc(student.getId()))
                .thenReturn(List.of(cancelled));

        List<InterviewResponse> result = interviewService.getMyInterviews(userId);

        assertThat(result).isEmpty();
    }

    @Test
    void getApplicationInterviews_byOwnerRecruiter_returnsList() {
        RecruiterProfile recruiter = recruiter();
        Application application = applicationFor(recruiter);
        when(recruiterProfileRepository.findByUserId(userId)).thenReturn(Optional.of(recruiter));
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(interviewRepository.findByApplicationIdOrderByRoundNumberAsc(applicationId)).thenReturn(List.of());

        List<InterviewResponse> result = interviewService.getApplicationInterviews(userId, applicationId);

        assertThat(result).isEmpty();
    }

    @Test
    void getApplicationInterviews_byNonOwnerRecruiter_throwsAccessDeniedException() {
        RecruiterProfile owner = recruiter();
        RecruiterProfile other = recruiter();
        Application application = applicationFor(owner);
        when(recruiterProfileRepository.findByUserId(userId)).thenReturn(Optional.of(other));
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> interviewService.getApplicationInterviews(userId, applicationId))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @Test
    void getAllInterviewsForAdmin_returnsPagedResponse() {
        when(interviewRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        interviewService.getAllInterviewsForAdmin(Pageable.unpaged());

        verify(interviewRepository).findAll(any(Pageable.class));
    }
}
