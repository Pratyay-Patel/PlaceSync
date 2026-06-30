package com.placesync.notification.consumer;

import com.placesync.common.event.*;
import com.placesync.notification.entity.NotificationType;
import com.placesync.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    private final NotificationService notificationService;

    @KafkaListener(topics = {"application-events", "interview-events", "offer-events"},
            groupId = "${spring.kafka.consumer.group-id:notification-group}")
    public void consume(ConsumerRecord<String, Object> record) {
        Object payload = record.value();
        try {
            dispatch(payload);
        } catch (Exception e) {
            log.error("Notification creation failed for Kafka payload type={}: {}",
                    payload != null ? payload.getClass().getSimpleName() : "null", e.getMessage(), e);
            throw e;
        }
    }

    private void dispatch(Object payload) {
        switch (payload) {
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

            default -> log.warn("Unhandled event type from Kafka: {}", payload.getClass().getName());
        }
    }
}
