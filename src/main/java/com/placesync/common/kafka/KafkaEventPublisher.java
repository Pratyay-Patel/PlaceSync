package com.placesync.common.kafka;

import com.placesync.common.event.*;
import com.placesync.common.metrics.PlaceSyncMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
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
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final PlaceSyncMetrics placeSyncMetrics;

    public KafkaEventPublisher(ApplicationEventPublisher applicationEventPublisher,
                               ObjectProvider<KafkaTemplate<String, Object>> kafkaTemplateProvider,
                               PlaceSyncMetrics placeSyncMetrics) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.kafkaTemplate = kafkaTemplateProvider.getIfAvailable();
        this.placeSyncMetrics = placeSyncMetrics;
    }

    public void publish(DomainEvent event) {
        applicationEventPublisher.publishEvent(event);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDomainEvent(DomainEvent event) {
        if (kafkaTemplate == null) {
            if (log.isDebugEnabled()) {
                log.debug("KafkaTemplate not available — routing {} via fallback", event.eventType());
            }
            placeSyncMetrics.recordKafkaFailure();
            applicationEventPublisher.publishEvent(new KafkaDeliveryFailedEvent(event));
            return;
        }
        String topic = resolveTopic(event);
        if (topic == null) {
            if (log.isWarnEnabled()) {
                log.warn("No topic mapping for event type: {}", event.eventType());
            }
            return;
        }
        kafkaTemplate.send(topic, event.eventId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.warn("Kafka publish failed for event={} topic={} — activating fallback",
                                event.eventType(), topic, ex);
                        placeSyncMetrics.recordKafkaFailure();
                        applicationEventPublisher.publishEvent(new KafkaDeliveryFailedEvent(event));
                    } else {
                        log.info("Published event={} to topic={}", event.eventType(), topic);
                    }
                });
    }

    private String resolveTopic(DomainEvent event) {
        return switch (event) {
            case ApplicationSubmittedEvent ignored -> KafkaTopics.APPLICATION_EVENTS;
            case ApplicationStatusChangedEvent ignored -> KafkaTopics.APPLICATION_EVENTS;
            case OfferReleasedEvent ignored -> KafkaTopics.OFFER_EVENTS;
            case InterviewScheduledEvent ignored -> KafkaTopics.INTERVIEW_EVENTS;
            case InterviewRescheduledEvent ignored -> KafkaTopics.INTERVIEW_EVENTS;
            case InterviewCancelledEvent ignored -> KafkaTopics.INTERVIEW_EVENTS;
            case RecruiterVerifiedEvent ignored -> KafkaTopics.APPLICATION_EVENTS;
            default -> null;
        };
    }
}
