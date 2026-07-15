package com.placesync.common.security;

import com.placesync.common.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RbacTest extends AbstractIntegrationTest {

    @Test
    void studentCannotCreateJob() throws Exception {
        String deadline = OffsetDateTime.now().plusDays(10).toString();
        String body = """
            {"title":"T","description":"D","locationType":"REMOTE","jobType":"FULL_TIME","applicationDeadline":"%s"}
            """.formatted(deadline);

        mockMvc.perform(post("/api/v1/jobs")
                        .header("Authorization", bearerHeader(studentUser))
                        .contentType("application/json").content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void recruiterCannotApplyToJob() throws Exception {
        var job = saveOpenJob();
        var resume = saveResume(studentProfile);
        String body = """
            {"jobId":"%s","resumeId":"%s"}
            """.formatted(job.getId(), resume.getId());

        mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", bearerHeader(recruiterUser))
                        .contentType("application/json").content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void recruiterCannotAccessStudentResumes() throws Exception {
        mockMvc.perform(get("/api/v1/students/resumes")
                        .header("Authorization", bearerHeader(recruiterUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentCannotAccessAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", bearerHeader(studentUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    void recruiterCannotAccessAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", bearerHeader(recruiterUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    void recruiterCannotListStudentApplications() throws Exception {
        mockMvc.perform(get("/api/v1/students/applications")
                        .header("Authorization", bearerHeader(recruiterUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanAccessAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", bearerHeader(adminUser)))
                .andExpect(status().isOk());
    }
}
