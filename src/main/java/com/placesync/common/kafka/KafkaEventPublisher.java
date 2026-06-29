package com.placesync.common.kafka;

import com.placesync.common.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class KafkaEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisher.class);

    private final ApplicationEventPublisher applicationEventPublisher;

    @Autowired(required = false)
    private KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public void publish(DomainEvent event) {
        applicationEventPublisher.publishEvent(event);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDomainEvent(DomainEvent event) {
        if (kafkaTemplate == null) {
            log.debug("KafkaTemplate not available — skipping Kafka publish for {}", event.eventType());
            return;
        }
        String topic = resolveTopic(event);
        if (topic == null) {
            log.warn("No topic mapping for event type: {}", event.eventType());
            return;
        }
        try {
            kafkaTemplate.send(topic, event.eventId().toString(), event);
            log.info("Published event={} to topic={}", event.eventType(), topic);
        } catch (Exception e) {
            log.warn("Kafka publish failed for event={} topic={} — notification service will use fallback",
                    event.eventType(), topic, e);
        }
    }

    private String resolveTopic(DomainEvent event) {
        return switch (event) {
            case ApplicationSubmittedEvent ignored -> "application-events";
            case ApplicationStatusChangedEvent ignored -> "application-events";
            case OfferReleasedEvent ignored -> "offer-events";
            case InterviewScheduledEvent ignored -> "interview-events";
            case InterviewRescheduledEvent ignored -> "interview-events";
            case InterviewCancelledEvent ignored -> "interview-events";
            case RecruiterVerifiedEvent ignored -> "application-events";
            default -> null;
        };
    }
}
