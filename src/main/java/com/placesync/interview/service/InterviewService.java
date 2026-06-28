package com.placesync.interview.service;

import com.placesync.application.entity.Application;
import com.placesync.application.entity.ApplicationStatus;
import com.placesync.application.repository.ApplicationRepository;
import com.placesync.common.exception.ConflictException;
import com.placesync.common.exception.ResourceNotFoundException;
import com.placesync.interview.dto.*;
import com.placesync.interview.entity.Interview;
import com.placesync.interview.entity.InterviewStatus;
import com.placesync.interview.repository.InterviewRepository;
import com.placesync.recruiter.entity.RecruiterProfile;
import com.placesync.recruiter.repository.RecruiterProfileRepository;
import com.placesync.user.entity.StudentProfile;
import com.placesync.user.repository.StudentProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InterviewService {

    private static final String RECRUITER_PROFILE = "RecruiterProfile";

    private final InterviewRepository interviewRepository;
    private final ApplicationRepository applicationRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final RecruiterProfileRepository recruiterProfileRepository;

    @Transactional(readOnly = true)
    public List<InterviewResponse> getMyInterviews(UUID userId) {
        StudentProfile student = studentProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("StudentProfile", userId));
        return interviewRepository.findByApplication_StudentIdOrderByScheduledAtAsc(student.getId())
                .stream()
                .filter(i -> i.getStatus() != InterviewStatus.CANCELLED)
                .map(InterviewResponse::from)
                .collect(Collectors.toList());
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
                .map(InterviewResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public InterviewResponse scheduleInterview(UUID userId, UUID applicationId, ScheduleInterviewRequest req) {
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

        return InterviewResponse.from(interviewRepository.save(interview));
    }

    @Transactional
    public InterviewResponse rescheduleInterview(UUID userId, UUID interviewId, UpdateInterviewRequest req) {
        Interview interview = loadInterviewForRecruiter(userId, interviewId);

        if (interview.getStatus() == InterviewStatus.CANCELLED
                || interview.getStatus() == InterviewStatus.COMPLETED) {
            throw new ConflictException("Cannot reschedule a " + interview.getStatus() + " interview");
        }

        interview.setScheduledAt(req.getScheduledAt());
        interview.setMeetingLink(req.getMeetingLink());
        interview.setVenue(req.getVenue());
        interview.setStatus(InterviewStatus.RESCHEDULED);
        return InterviewResponse.from(interviewRepository.save(interview));
    }

    @Transactional
    public InterviewResponse cancelInterview(UUID userId, UUID interviewId, CancelInterviewRequest req) {
        Interview interview = loadInterviewForRecruiter(userId, interviewId);

        if (interview.getStatus() == InterviewStatus.CANCELLED) {
            throw new ConflictException("Interview is already cancelled");
        }
        if (interview.getStatus() == InterviewStatus.COMPLETED) {
            throw new ConflictException("Cannot cancel a completed interview");
        }

        interview.setStatus(InterviewStatus.CANCELLED);
        interview.setCancellationReason(req.getCancellationReason());
        return InterviewResponse.from(interviewRepository.save(interview));
    }

    @Transactional
    public InterviewResponse completeInterview(UUID userId, UUID interviewId) {
        Interview interview = loadInterviewForRecruiter(userId, interviewId);

        if (interview.getStatus() != InterviewStatus.SCHEDULED
                && interview.getStatus() != InterviewStatus.RESCHEDULED) {
            throw new ConflictException("Only SCHEDULED or RESCHEDULED interviews can be marked as completed");
        }

        interview.setStatus(InterviewStatus.COMPLETED);
        return InterviewResponse.from(interviewRepository.save(interview));
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
