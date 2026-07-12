package com.placesync.common.metrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlaceSyncMetricsTest {

    private SimpleMeterRegistry registry;
    private PlaceSyncMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new PlaceSyncMetrics(registry);
    }

    @Test
    void recordApplication_incrementsCounter() {
        metrics.recordApplication();
        metrics.recordApplication();
        assertThat(registry.get("placesync.jobs.applied.total").counter().count()).isEqualTo(2.0);
    }

    @Test
    void recordInterview_incrementsCounter() {
        metrics.recordInterview();
        assertThat(registry.get("placesync.interviews.scheduled.total").counter().count()).isEqualTo(1.0);
    }

    @Test
    void recordKafkaFailure_incrementsCounter() {
        metrics.recordKafkaFailure();
        assertThat(registry.get("placesync.kafka.publish.failures.total").counter().count()).isEqualTo(1.0);
    }

    @Test
    void recordEmailFailure_incrementsCounter() {
        metrics.recordEmailFailure();
        assertThat(registry.get("placesync.email.send.failures.total").counter().count()).isEqualTo(1.0);
    }

    @Test
    void cacheHitRatio_withNoRequests_returnsZero() {
        double ratio = registry.get("placesync.cache.hit.ratio").gauge().value();
        assertThat(ratio).isEqualTo(0.0);
    }

    @Test
    void cacheHitRatio_withHitsAndMisses_returnsCorrectRatio() {
        metrics.recordCacheHit();
        metrics.recordCacheHit();
        metrics.recordCacheHit();
        metrics.recordCacheMiss();
        double ratio = registry.get("placesync.cache.hit.ratio").gauge().value();
        assertThat(ratio).isEqualTo(0.75);
    }

    @Test
    void cacheHitRatio_allHits_returnsOne() {
        metrics.recordCacheHit();
        metrics.recordCacheHit();
        double ratio = registry.get("placesync.cache.hit.ratio").gauge().value();
        assertThat(ratio).isEqualTo(1.0);
    }

    @Test
    void cacheHitRatio_allMisses_returnsZero() {
        metrics.recordCacheMiss();
        metrics.recordCacheMiss();
        double ratio = registry.get("placesync.cache.hit.ratio").gauge().value();
        assertThat(ratio).isEqualTo(0.0);
    }
}
