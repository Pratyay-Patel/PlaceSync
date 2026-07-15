package com.placesync.auth.controller;

import com.placesync.common.AbstractIntegrationTest;
import com.placesync.user.entity.User;
import com.placesync.user.entity.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerIT extends AbstractIntegrationTest {

    private static final String AUTH_BASE = "/api/v1/auth";
    private static final String TEST_PASSWORD = "Password1!";

    @Test
    void register_validStudent_returns201() throws Exception {
        String body = """
            {
              "email": "newstudent@test.com",
              "password": "Password1!",
              "role": "ROLE_STUDENT",
              "firstName": "New",
              "lastName": "Student",
              "institution": "IIT",
              "department": "CS",
              "graduationYear": 2026
            }
            """;

        mockMvc.perform(post(AUTH_BASE + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        String body = """
            {
              "email": "student@itest.com",
              "password": "Password1!",
              "role": "ROLE_STUDENT",
              "firstName": "Dupe",
              "lastName": "User",
              "institution": "IIT",
              "department": "CS",
              "graduationYear": 2026
            }
            """;

        mockMvc.perform(post(AUTH_BASE + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void login_validCredentials_returns200WithTokens() throws Exception {
        String hash = new BCryptPasswordEncoder(4).encode(TEST_PASSWORD);
        userRepository.save(User.builder()
                .email("login@test.com").passwordHash(hash)
                .role(UserRole.ROLE_STUDENT).build());

        String body = """
            {"email":"login@test.com","password":"Password1!"}
            """;

        mockMvc.perform(post(AUTH_BASE + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void login_invalidPassword_returns401() throws Exception {
        String hash = new BCryptPasswordEncoder(4).encode(TEST_PASSWORD);
        userRepository.save(User.builder()
                .email("loginbad@test.com").passwordHash(hash)
                .role(UserRole.ROLE_STUDENT).build());

        String body = """
            {"email":"loginbad@test.com","password":"WrongPassword1!"}
            """;

        mockMvc.perform(post(AUTH_BASE + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_authenticated_returnsUserInfo() throws Exception {
        mockMvc.perform(get(AUTH_BASE + "/me")
                        .header("Authorization", bearerHeader(studentUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("student@itest.com"));
    }

    @Test
    void me_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get(AUTH_BASE + "/me"))
                .andExpect(status().isUnauthorized());
    }
}
