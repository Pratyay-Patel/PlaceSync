package com.placesync.notification.consumer;

import com.placesync.common.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationDispatcher dispatcher;

    @KafkaListener(topics = {KafkaTopics.APPLICATION_EVENTS, KafkaTopics.INTERVIEW_EVENTS, KafkaTopics.OFFER_EVENTS},
            groupId = "${spring.kafka.consumer.group-id:notification-group}")
    public void consume(ConsumerRecord<String, Object> consumerRecord) {
        Object payload = consumerRecord.value();
        try {
            dispatcher.dispatch(payload);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to create notification for payload type="
                    + (payload != null ? payload.getClass().getSimpleName() : "null"), e);
        }
    }
}
