package com.placesync.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class PlaceSyncMetrics {

    private final Counter applicationsTotal;
    private final Counter interviewsTotal;
    private final Counter kafkaFailures;
    private final Counter emailFailures;
    private final AtomicLong cacheHits = new AtomicLong();
    private final AtomicLong cacheMisses = new AtomicLong();

    public PlaceSyncMetrics(MeterRegistry registry) {
        this.applicationsTotal = Counter.builder("placesync.jobs.applied.total")
                .description("Total job applications submitted")
                .register(registry);
        this.interviewsTotal = Counter.builder("placesync.interviews.scheduled.total")
                .description("Total interviews scheduled")
                .register(registry);
        this.kafkaFailures = Counter.builder("placesync.kafka.publish.failures.total")
                .description("Kafka publish failures that fell back to Spring Events")
                .register(registry);
        this.emailFailures = Counter.builder("placesync.email.send.failures.total")
                .description("Email delivery failures")
                .register(registry);
        Gauge.builder("placesync.cache.hit.ratio", this, m -> {
            long total = m.cacheHits.get() + m.cacheMisses.get();
            return total == 0 ? 0.0 : (double) m.cacheHits.get() / total;
        }).description("Redis cache hit ratio").register(registry);
    }

    public void recordApplication() { applicationsTotal.increment(); }
    public void recordInterview() { interviewsTotal.increment(); }
    public void recordKafkaFailure() { kafkaFailures.increment(); }
    public void recordEmailFailure() { emailFailures.increment(); }
    public void recordCacheHit() { cacheHits.incrementAndGet(); }
    public void recordCacheMiss() { cacheMisses.incrementAndGet(); }
}
