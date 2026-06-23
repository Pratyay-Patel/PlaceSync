package com.placesync;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class PlaceSyncApplicationTests {

    @Test
    void contextLoads() {
        // Verifies the Spring application context starts without errors.
        // Profile 'test' uses H2 in-memory database with Flyway disabled.
    }
}
