package com.placesync.job.controller;

import com.placesync.common.AbstractIntegrationTest;
import com.placesync.job.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.OffsetDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class JobControllerIT extends AbstractIntegrationTest {

    private static final String JOBS_URL = "/api/v1/jobs";

    @Test
    void getOpenJobs_noToken_returns401() throws Exception {
        mockMvc.perform(get(JOBS_URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getOpenJobs_authenticated_returns200() throws Exception {
        mockMvc.perform(get(JOBS_URL)
                        .header("Authorization", bearerHeader(studentUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void createJob_asRecruiter_returns201() throws Exception {
        String deadline = OffsetDateTime.now().plusDays(30).toString();
        String body = """
            {
              "title": "Backend Engineer",
              "description": "Build APIs",
              "locationType": "REMOTE",
              "jobType": "FULL_TIME",
              "applicationDeadline": "%s"
            }
            """.formatted(deadline);

        mockMvc.perform(post(JOBS_URL)
                        .header("Authorization", bearerHeader(recruiterUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Backend Engineer"));
    }

    @Test
    void createJob_asStudent_returns403() throws Exception {
        String deadline = OffsetDateTime.now().plusDays(30).toString();
        String body = """
            {
              "title": "Hacker Job",
              "description": "Bypass auth",
              "locationType": "REMOTE",
              "jobType": "FULL_TIME",
              "applicationDeadline": "%s"
            }
            """.formatted(deadline);

        mockMvc.perform(post(JOBS_URL)
                        .header("Authorization", bearerHeader(studentUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void getJob_authenticated_returnsJobDetail() throws Exception {
        var job = saveOpenJob();

        mockMvc.perform(get(JOBS_URL + "/" + job.getId())
                        .header("Authorization", bearerHeader(studentUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(job.getId().toString()));
    }

    @Test
    void getJob_noToken_returns401() throws Exception {
        var job = saveOpenJob();

        mockMvc.perform(get(JOBS_URL + "/" + job.getId()))
                .andExpect(status().isUnauthorized());
    }
}
