package com.placesync.notification.consumer;

import com.placesync.common.event.*;
import com.placesync.notification.entity.NotificationType;
import com.placesync.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class NotificationFallbackListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationFallbackListener.class);

    private final NotificationService notificationService;

    @EventListener
    @Transactional
    public void onKafkaDeliveryFailed(KafkaDeliveryFailedEvent wrapper) {
        DomainEvent event = wrapper.domainEvent();
        log.warn("Kafka delivery failed — creating notification via fallback for event={}", event.eventType());
        try {
            dispatch(event);
        } catch (Exception e) {
            log.error("Fallback notification creation failed for event={}: {}", event.eventType(), e.getMessage(), e);
        }
    }

    private void dispatch(DomainEvent event) {
        switch (event) {
            case ApplicationSubmittedEvent e -> notificationService.createForUser(
                    e.studentId(), NotificationType.APPLICATION_SUBMITTED,
                    "Application submitted",
                    "Your application for " + e.jobTitle() + " at " + e.companyName() + " was received.",
                    e.applicationId(), "Application");

            case ApplicationStatusChangedEvent e -> notificationService.createForUser(
                    e.studentId(), NotificationType.APPLICATION_STATUS_CHANGED,
                    "Application status updated",
                    "Your application status is now " + e.newStatus() + ".",
                    e.applicationId(), "Application");

            case OfferReleasedEvent e -> notificationService.createForUser(
                    e.studentId(), NotificationType.OFFER_RELEASED,
                    "Offer received!",
                    "Congratulations! You have received an offer from " + e.companyName() + " for " + e.jobTitle() + ".",
                    e.applicationId(), "Application");

            case InterviewScheduledEvent e -> notificationService.createForUser(
                    e.studentId(), NotificationType.INTERVIEW_SCHEDULED,
                    "Interview scheduled",
                    "Round " + e.round() + " interview scheduled for " + e.scheduledAt() + ".",
                    e.interviewId(), "Interview");

            case InterviewRescheduledEvent e -> notificationService.createForUser(
                    e.studentId(), NotificationType.INTERVIEW_RESCHEDULED,
                    "Interview rescheduled",
                    "Your interview has been moved to " + e.newScheduledAt() + ".",
                    e.interviewId(), "Interview");

            case InterviewCancelledEvent e -> notificationService.createForUser(
                    e.studentId(), NotificationType.INTERVIEW_CANCELLED,
                    "Interview cancelled",
                    "Your interview was cancelled: " + e.cancellationReason() + ".",
                    e.interviewId(), "Interview");

            case RecruiterVerifiedEvent e -> {
                if ("APPROVE".equals(e.decision())) {
                    notificationService.createForUser(e.userId(), NotificationType.RECRUITER_VERIFIED,
                            "Verification approved",
                            "Your recruiter profile has been verified.",
                            e.recruiterId(), "RecruiterProfile");
                } else {
                    notificationService.createForUser(e.userId(), NotificationType.RECRUITER_REJECTED,
                            "Verification rejected",
                            "Your recruiter verification was rejected.",
                            e.recruiterId(), "RecruiterProfile");
                }
            }

            default -> log.warn("Unhandled event type in fallback listener: {}", event.getClass().getName());
        }
    }
}
