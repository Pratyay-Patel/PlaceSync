package com.placesync.notification.consumer;

import com.placesync.auth.service.EmailService;
import com.placesync.common.event.*;
import com.placesync.common.kafka.KafkaTopics;
import com.placesync.notification.entity.NotificationType;
import com.placesync.notification.service.NotificationService;
import com.placesync.user.entity.User;
import com.placesync.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    private static final String REF_APPLICATION = "Application";
    private static final String REF_INTERVIEW = "Interview";

    private final NotificationService notificationService;
    private final EmailService emailService;
    private final UserRepository userRepository;

    @KafkaListener(topics = {KafkaTopics.APPLICATION_EVENTS, KafkaTopics.INTERVIEW_EVENTS, KafkaTopics.OFFER_EVENTS},
            groupId = "${spring.kafka.consumer.group-id:notification-group}")
    public void consume(ConsumerRecord<String, Object> consumerRecord) {
        Object payload = consumerRecord.value();
        try {
            dispatch(payload);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to create notification for payload type="
                    + (payload != null ? payload.getClass().getSimpleName() : "null"), e);
        }
    }

    private void dispatch(Object payload) {
        switch (payload) {
            case ApplicationSubmittedEvent e -> {
                notificationService.createForUser(
                        e.studentId(), NotificationType.APPLICATION_SUBMITTED,
                        "Application submitted",
                        "Your application for " + e.jobTitle() + " at " + e.companyName() + " was received.",
                        e.applicationId(), REF_APPLICATION);
                emailService.sendApplicationConfirmation(e.studentEmail(), e.jobTitle(), e.companyName());
            }

            case ApplicationStatusChangedEvent e -> {
                notificationService.createForUser(
                        e.studentId(), NotificationType.APPLICATION_STATUS_CHANGED,
                        "Application status updated",
                        "Your application status is now " + e.newStatus() + ".",
                        e.applicationId(), REF_APPLICATION);
                String email = findEmail(e.studentId());
                if (email != null) emailService.sendApplicationStatusUpdate(email, e.newStatus().name());
            }

            case OfferReleasedEvent e -> {
                notificationService.createForUser(
                        e.studentId(), NotificationType.OFFER_RELEASED,
                        "Offer received!",
                        "Congratulations! You have received an offer from " + e.companyName() + " for " + e.jobTitle() + ".",
                        e.applicationId(), REF_APPLICATION);
                String email = findEmail(e.studentId());
                if (email != null) emailService.sendApplicationStatusUpdate(email, "OFFERED");
            }

            case InterviewScheduledEvent e -> {
                notificationService.createForUser(
                        e.studentId(), NotificationType.INTERVIEW_SCHEDULED,
                        "Interview scheduled",
                        "Round " + e.round() + " interview scheduled for " + e.scheduledAt() + ".",
                        e.interviewId(), REF_INTERVIEW);
                String email = findEmail(e.studentId());
                if (email != null) emailService.sendInterviewScheduled(
                        email, e.round(), e.scheduledAt().toString(), e.meetingLink());
            }

            case InterviewRescheduledEvent e -> {
                notificationService.createForUser(
                        e.studentId(), NotificationType.INTERVIEW_RESCHEDULED,
                        "Interview rescheduled",
                        "Your interview has been moved to " + e.newScheduledAt() + ".",
                        e.interviewId(), REF_INTERVIEW);
                String email = findEmail(e.studentId());
                if (email != null) emailService.sendInterviewRescheduled(
                        email, 0, e.newScheduledAt().toString());
            }

            case InterviewCancelledEvent e -> {
                notificationService.createForUser(
                        e.studentId(), NotificationType.INTERVIEW_CANCELLED,
                        "Interview cancelled",
                        "Your interview was cancelled: " + e.cancellationReason() + ".",
                        e.interviewId(), REF_INTERVIEW);
                String email = findEmail(e.studentId());
                if (email != null) emailService.sendInterviewCancelled(email, e.cancellationReason());
            }

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

    private String findEmail(UUID userId) {
        return userRepository.findById(userId).map(User::getEmail).orElse(null);
    }
}
