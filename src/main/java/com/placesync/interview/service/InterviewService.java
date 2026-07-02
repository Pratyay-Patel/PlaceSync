package com.placesync.interview.service;

import com.placesync.application.entity.Application;
import com.placesync.application.entity.ApplicationStatus;
import com.placesync.application.repository.ApplicationRepository;
import com.placesync.common.audit.AuditAction;
import com.placesync.common.audit.Auditable;
import com.placesync.common.event.InterviewCancelledEvent;
import com.placesync.common.event.InterviewRescheduledEvent;
import com.placesync.common.event.InterviewScheduledEvent;
import com.placesync.common.exception.ConflictException;
import com.placesync.common.exception.ResourceNotFoundException;
import com.placesync.common.kafka.KafkaEventPublisher;
import com.placesync.common.util.PagedResponse;
import com.placesync.interview.dto.*;
import org.springframework.data.domain.Pageable;
import com.placesync.interview.entity.Interview;
import com.placesync.interview.mapper.InterviewMapper;
import com.placesync.interview.entity.InterviewStatus;
import com.placesync.interview.repository.InterviewRepository;
import com.placesync.recruiter.entity.RecruiterProfile;
import com.placesync.recruiter.repository.RecruiterProfileRepository;
import com.placesync.user.entity.StudentProfile;
import com.placesync.user.repository.StudentProfileRepository;
import static com.placesync.common.util.LogSanitizer.sanitize;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@SuppressWarnings("java:S2629")
@Service
@RequiredArgsConstructor
public class InterviewService {

    private static final Logger log = LoggerFactory.getLogger(InterviewService.class);
    private static final String RECRUITER_PROFILE = "RecruiterProfile";

    private final InterviewRepository interviewRepository;
    private final InterviewMapper interviewMapper;
    private final ApplicationRepository applicationRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final RecruiterProfileRepository recruiterProfileRepository;
    private final KafkaEventPublisher kafkaEventPublisher;

    @Transactional(readOnly = true)
    public List<InterviewResponse> getMyInterviews(UUID userId) {
        StudentProfile student = studentProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("StudentProfile", userId));
        return interviewRepository.findByApplication_StudentIdOrderByScheduledAtAsc(student.getId())
                .stream()
                .filter(i -> i.getStatus() != InterviewStatus.CANCELLED)
                .map(interviewMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InterviewResponse> getApplicationInterviews(UUID userId, UUID applicationId) {
        RecruiterProfile recruiter = recruiterProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(RECRUITER_PROFILE, userId));

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", applicationId));

        if (!application.getJob().getRecruiter().getId().equals(recruiter.getId())) {
            throw new AccessDeniedException("Application does not belong to the recruiter's job");
        }
        return interviewRepository.findByApplicationIdOrderByRoundNumberAsc(applicationId)
                .stream()
                .map(interviewMapper::toResponse)
                .toList();
    }

    @Auditable(action = AuditAction.CREATE, entityType = "Interview")
    @Transactional
    public InterviewResponse scheduleInterview(UUID userId, UUID applicationId, ScheduleInterviewRequest req) {
        log.info("Scheduling interview round {} for applicationId={} by userId={}", sanitize(req.getRoundNumber()), sanitize(applicationId), sanitize(userId));
        RecruiterProfile recruiter = recruiterProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(RECRUITER_PROFILE, userId));

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", applicationId));

        if (!application.getJob().getRecruiter().getId().equals(recruiter.getId())) {
            throw new AccessDeniedException("Application does not belong to the recruiter's job");
        }
        if (interviewRepository.existsByApplicationIdAndRoundNumber(applicationId, req.getRoundNumber())) {
            throw new ConflictException("Interview round " + req.getRoundNumber() + " already exists for this application");
        }

        Interview interview = Interview.builder()
                .application(application)
                .roundNumber(req.getRoundNumber())
                .interviewType(req.getInterviewType())
                .scheduledAt(req.getScheduledAt())
                .durationMinutes(req.getDurationMinutes())
                .meetingLink(req.getMeetingLink())
                .venue(req.getVenue())
                .status(InterviewStatus.SCHEDULED)
                .build();

        application.setStatus(ApplicationStatus.INTERVIEW_SCHEDULED);
        applicationRepository.save(application);

        Interview saved = interviewRepository.save(interview);
        kafkaEventPublisher.publish(InterviewScheduledEvent.of(
                saved.getId(), applicationId, application.getStudent().getUser().getId(),
                req.getRoundNumber(), req.getScheduledAt(), req.getMeetingLink()));
        return interviewMapper.toResponse(saved);
    }

    @Transactional
    public InterviewResponse rescheduleInterview(UUID userId, UUID interviewId, UpdateInterviewRequest req) {
        log.info("Rescheduling interviewId={} by userId={}", sanitize(interviewId), sanitize(userId));
        Interview interview = loadInterviewForRecruiter(userId, interviewId);

        if (interview.getStatus() == InterviewStatus.CANCELLED
                || interview.getStatus() == InterviewStatus.COMPLETED) {
            throw new ConflictException("Cannot reschedule a " + interview.getStatus() + " interview");
        }

        java.time.OffsetDateTime oldScheduledAt = interview.getScheduledAt();
        interview.setScheduledAt(req.getScheduledAt());
        interview.setMeetingLink(req.getMeetingLink());
        interview.setVenue(req.getVenue());
        interview.setStatus(InterviewStatus.RESCHEDULED);
        Interview saved = interviewRepository.save(interview);
        kafkaEventPublisher.publish(InterviewRescheduledEvent.of(
                saved.getId(), saved.getApplication().getStudent().getUser().getId(),
                oldScheduledAt, req.getScheduledAt()));
        return interviewMapper.toResponse(saved);
    }

    @Auditable(action = AuditAction.UPDATE, entityType = "Interview")
    @Transactional
    public InterviewResponse cancelInterview(UUID userId, UUID interviewId, CancelInterviewRequest req) {
        log.info("Cancelling interviewId={} by userId={}", sanitize(interviewId), sanitize(userId));
        Interview interview = loadInterviewForRecruiter(userId, interviewId);

        if (interview.getStatus() == InterviewStatus.CANCELLED) {
            throw new ConflictException("Interview is already cancelled");
        }
        if (interview.getStatus() == InterviewStatus.COMPLETED) {
            throw new ConflictException("Cannot cancel a completed interview");
        }

        interview.setStatus(InterviewStatus.CANCELLED);
        interview.setCancellationReason(req.getCancellationReason());
        Interview saved = interviewRepository.save(interview);
        kafkaEventPublisher.publish(InterviewCancelledEvent.of(
                saved.getId(), saved.getApplication().getStudent().getUser().getId(),
                req.getCancellationReason()));
        return interviewMapper.toResponse(saved);
    }

    @Transactional
    public InterviewResponse completeInterview(UUID userId, UUID interviewId) {
        log.info("Completing interviewId={} by userId={}", sanitize(interviewId), sanitize(userId));
        Interview interview = loadInterviewForRecruiter(userId, interviewId);

        if (interview.getStatus() != InterviewStatus.SCHEDULED
                && interview.getStatus() != InterviewStatus.RESCHEDULED) {
            throw new ConflictException("Only SCHEDULED or RESCHEDULED interviews can be marked as completed");
        }

        interview.setStatus(InterviewStatus.COMPLETED);
        return interviewMapper.toResponse(interviewRepository.save(interview));
    }

    @Transactional(readOnly = true)
    public PagedResponse<InterviewResponse> getAllInterviewsForAdmin(Pageable pageable) {
        return PagedResponse.of(interviewRepository.findAll(pageable).map(interviewMapper::toResponse));
    }

    private Interview loadInterviewForRecruiter(UUID userId, UUID interviewId) {
        RecruiterProfile recruiter = recruiterProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(RECRUITER_PROFILE, userId));

        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview", interviewId));

        if (!interview.getApplication().getJob().getRecruiter().getId().equals(recruiter.getId())) {
            throw new AccessDeniedException("Interview does not belong to the recruiter's job");
        }
        return interview;
    }
}
