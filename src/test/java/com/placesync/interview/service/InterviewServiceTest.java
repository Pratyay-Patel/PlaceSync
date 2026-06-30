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
import java.util.Optional;
import java.util.UUID;

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

        assertThatThrownBy(() -> interviewService.scheduleInterview(userId, applicationId, new ScheduleInterviewRequest()))
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

        assertThatThrownBy(() -> interviewService.rescheduleInterview(userId, interviewId, new UpdateInterviewRequest()))
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

        assertThatThrownBy(() -> interviewService.cancelInterview(userId, interviewId, new CancelInterviewRequest()))
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

        assertThatThrownBy(() -> interviewService.cancelInterview(userId, interviewId, new CancelInterviewRequest()))
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

        assertThatThrownBy(() -> interviewService.scheduleInterview(userId, applicationId, new ScheduleInterviewRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
