package com.placesync.application.controller;

import com.placesync.application.entity.Application;
import com.placesync.application.entity.ApplicationStatus;
import com.placesync.application.repository.ApplicationRepository;
import com.placesync.common.AbstractIntegrationTest;
import com.placesync.job.entity.Job;
import com.placesync.user.entity.Resume;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApplicationControllerIT extends AbstractIntegrationTest {

    @Autowired private ApplicationRepository applicationRepository;

    Job openJob;
    Resume resume;

    @BeforeEach
    void setUpJobAndResume() {
        openJob = saveOpenJob();
        resume = saveResume(studentProfile);
    }

    @Test
    void apply_asStudent_returns201() throws Exception {
        String body = """
            {"jobId":"%s","resumeId":"%s"}
            """.formatted(openJob.getId(), resume.getId());

        mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", bearerHeader(studentUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("APPLIED"));
    }

    @Test
    void apply_asRecruiter_returns403() throws Exception {
        String body = """
            {"jobId":"%s","resumeId":"%s"}
            """.formatted(openJob.getId(), resume.getId());

        mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", bearerHeader(recruiterUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void apply_noToken_returns401() throws Exception {
        String body = """
            {"jobId":"%s","resumeId":"%s"}
            """.formatted(openJob.getId(), resume.getId());

        mockMvc.perform(post("/api/v1/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMyApplications_asStudent_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/students/applications")
                        .header("Authorization", bearerHeader(studentUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getMyApplications_asRecruiter_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/students/applications")
                        .header("Authorization", bearerHeader(recruiterUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMyApplication_asOwner_returns200() throws Exception {
        Application app = applicationRepository.save(Application.builder()
                .student(studentProfile).job(openJob).resume(resume)
                .status(ApplicationStatus.APPLIED).build());

        mockMvc.perform(get("/api/v1/students/applications/" + app.getId())
                        .header("Authorization", bearerHeader(studentUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(app.getId().toString()));
    }
}
