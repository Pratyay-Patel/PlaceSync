package com.placesync.user.controller;

import com.placesync.common.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ResumeControllerIT extends AbstractIntegrationTest {

    private static final String RESUMES_URL = "/api/v1/students/resumes";

    @Test
    void getMyResumes_asStudent_returns200() throws Exception {
        mockMvc.perform(get(RESUMES_URL)
                        .header("Authorization", bearerHeader(studentUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getMyResumes_withUploadedResume_returnsIt() throws Exception {
        saveResume(studentProfile);

        mockMvc.perform(get(RESUMES_URL)
                        .header("Authorization", bearerHeader(studentUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].label").value("CV"));
    }

    @Test
    void getMyResumes_asRecruiter_returns403() throws Exception {
        mockMvc.perform(get(RESUMES_URL)
                        .header("Authorization", bearerHeader(recruiterUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMyResumes_noToken_returns401() throws Exception {
        mockMvc.perform(get(RESUMES_URL))
                .andExpect(status().isUnauthorized());
    }
}
