package com.placesync.common.security;

import com.placesync.common.AbstractIntegrationTest;
import com.placesync.user.entity.UserRole;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class JwtValidationTest extends AbstractIntegrationTest {

    private static final String PROTECTED = "/api/v1/jobs";
    private static final String TEST_SECRET = "dev-secret-key-change-in-production-32ch";

    @Test
    void missingToken_returns401() throws Exception {
        mockMvc.perform(get(PROTECTED))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void malformedAuthorizationHeader_returns401() throws Exception {
        mockMvc.perform(get(PROTECTED)
                        .header("Authorization", "NotBearer xyz"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredToken_returns401() throws Exception {
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        String expired = Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("email", "exp@test.com")
                .claim("role", UserRole.ROLE_STUDENT.name())
                .issuedAt(Date.from(Instant.now().minusSeconds(3600)))
                .expiration(Date.from(Instant.now().minusSeconds(1)))
                .issuer("placesync")
                .signWith(key)
                .compact();

        mockMvc.perform(get(PROTECTED)
                        .header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tamperedToken_returns401() throws Exception {
        String validToken = jwtTokenProvider.generateAccessToken(
                studentUser.getId(), studentUser.getEmail(), UserRole.ROLE_STUDENT);
        String tampered = validToken.substring(0, validToken.length() - 4) + "XXXX";

        mockMvc.perform(get(PROTECTED)
                        .header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenSignedWithWrongKey_returns401() throws Exception {
        SecretKey wrongKey = Keys.hmacShaKeyFor(
                "different-secret-key-must-be-32-chars-ok!".getBytes(StandardCharsets.UTF_8));
        String badToken = Jwts.builder()
                .subject(studentUser.getId().toString())
                .claim("email", studentUser.getEmail())
                .claim("role", UserRole.ROLE_STUDENT.name())
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(900)))
                .issuer("placesync")
                .signWith(wrongKey)
                .compact();

        mockMvc.perform(get(PROTECTED)
                        .header("Authorization", "Bearer " + badToken))
                .andExpect(status().isUnauthorized());
    }
}
