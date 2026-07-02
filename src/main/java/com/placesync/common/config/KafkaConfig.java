package com.placesync.common.config;

import com.placesync.common.kafka.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic applicationEventsTopic() {
        return TopicBuilder.name(KafkaTopics.APPLICATION_EVENTS).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic interviewEventsTopic() {
        return TopicBuilder.name(KafkaTopics.INTERVIEW_EVENTS).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic offerEventsTopic() {
        return TopicBuilder.name(KafkaTopics.OFFER_EVENTS).partitions(3).replicas(1).build();
    }

    @Bean
    @ConditionalOnBean(KafkaTemplate.class)
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3));
    }
}
