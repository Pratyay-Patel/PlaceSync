package com.placesync.common.security;

import com.placesync.auth.entity.RefreshToken;
import com.placesync.auth.repository.RefreshTokenRepository;
import com.placesync.auth.service.AuthService;
import com.placesync.common.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RefreshTokenSecurityTest extends AbstractIntegrationTest {

    @Autowired private RefreshTokenRepository refreshTokenRepository;

    private static final String REFRESH_URL = "/api/v1/auth/refresh";

    private RefreshToken storeActiveToken(String rawToken) {
        return refreshTokenRepository.save(RefreshToken.builder()
                .user(studentUser)
                .tokenHash(AuthService.hashToken(rawToken))
                .familyId(UUID.randomUUID())
                .expiresAt(OffsetDateTime.now().plusDays(7))
                .isRevoked(false)
                .build());
    }

    @Test
    void refresh_withValidToken_returns200AndNewTokens() throws Exception {
        String rawToken = UUID.randomUUID().toString();
        storeActiveToken(rawToken);
        String body = """
            {"refreshToken":"%s"}
            """.formatted(rawToken);

        mockMvc.perform(post(REFRESH_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void refresh_withRevokedToken_returns401() throws Exception {
        String rawToken = UUID.randomUUID().toString();
        RefreshToken token = storeActiveToken(rawToken);
        token.setIsRevoked(true);
        token.setRevokedAt(OffsetDateTime.now());
        refreshTokenRepository.save(token);

        String body = """
            {"refreshToken":"%s"}
            """.formatted(rawToken);

        mockMvc.perform(post(REFRESH_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_withNonExistentToken_returns401() throws Exception {
        String body = """
            {"refreshToken":"%s"}
            """.formatted(UUID.randomUUID());

        mockMvc.perform(post(REFRESH_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_reuseOfRotatedToken_returns401() throws Exception {
        String originalRaw = UUID.randomUUID().toString();
        storeActiveToken(originalRaw);
        String body = """
            {"refreshToken":"%s"}
            """.formatted(originalRaw);

        mockMvc.perform(post(REFRESH_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(post(REFRESH_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }
}
