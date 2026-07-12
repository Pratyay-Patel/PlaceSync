package com.placesync.common.security;

import com.placesync.common.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityFilterChainTest extends AbstractIntegrationTest {

    @Test
    void authEndpoints_withoutToken_arePublic() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"x@x.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void actuatorHealth_withoutToken_returns200() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void actuatorPrometheus_withoutToken_isNotBlocked() throws Exception {
        // Security assertion: Spring Security must not return 401/403 for /actuator/prometheus
        // (Prometheus scraping must not require authentication).
        // The endpoint may not be registered in the MockMvc test context (Spring Boot 3.3 + Prometheus
        // client 1.x), so we assert on the security decision only, not on endpoint availability.
        int status = mockMvc.perform(get("/actuator/prometheus"))
                .andReturn().getResponse().getStatus();
        assertThat(status).as("Prometheus path must not be blocked by security").isNotIn(401, 403);
    }

    @Test
    void protectedEndpoint_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/jobs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_withToken_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/jobs")
                        .header("Authorization", bearerHeader(studentUser)))
                .andExpect(status().isOk());
    }

    @Test
    void notifications_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isUnauthorized());
    }
}
