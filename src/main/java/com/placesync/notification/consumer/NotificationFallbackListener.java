package com.placesync.notification.consumer;

import com.placesync.common.event.DomainEvent;
import com.placesync.common.event.KafkaDeliveryFailedEvent;
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

    private final NotificationDispatcher dispatcher;

    @EventListener
    @Transactional
    public void onKafkaDeliveryFailed(KafkaDeliveryFailedEvent wrapper) {
        DomainEvent event = wrapper.domainEvent();
        if (log.isWarnEnabled()) {
            log.warn("Kafka delivery failed — creating notification via fallback for event={}", event.eventType());
        }
        try {
            dispatcher.dispatch(event);
        } catch (Exception e) {
            log.error("Fallback notification creation failed for event={}: {}", event.eventType(), e.getMessage(), e);
        }
    }
}
