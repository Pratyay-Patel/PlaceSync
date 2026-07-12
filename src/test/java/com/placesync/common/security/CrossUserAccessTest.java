package com.placesync.common.security;

import com.placesync.application.entity.Application;
import com.placesync.application.entity.ApplicationStatus;
import com.placesync.application.repository.ApplicationRepository;
import com.placesync.common.AbstractIntegrationTest;
import com.placesync.user.entity.StudentProfile;
import com.placesync.user.entity.User;
import com.placesync.user.entity.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CrossUserAccessTest extends AbstractIntegrationTest {

    @Autowired private ApplicationRepository applicationRepository;

    @Test
    void studentA_cannotAccessStudentB_application() throws Exception {
        User studentB = userRepository.save(User.builder()
                .email("studentb@itest.com").passwordHash("hash")
                .role(UserRole.ROLE_STUDENT).build());
        StudentProfile profileB = studentProfileRepository.save(StudentProfile.builder()
                .user(studentB).firstName("Bob").lastName("B")
                .institution("NIT").department("ME").graduationYear((short) 2025).build());

        var job = saveOpenJob();
        var resumeB = saveResume(profileB);
        Application appB = applicationRepository.save(Application.builder()
                .student(profileB).job(job).resume(resumeB).status(ApplicationStatus.APPLIED).build());

        mockMvc.perform(get("/api/v1/students/applications/" + appB.getId())
                        .header("Authorization", bearerHeader(studentUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentA_listingOwnApplications_doesNotSeeStudentB_applications() throws Exception {
        User studentB = userRepository.save(User.builder()
                .email("studentb2@itest.com").passwordHash("hash")
                .role(UserRole.ROLE_STUDENT).build());
        StudentProfile profileB = studentProfileRepository.save(StudentProfile.builder()
                .user(studentB).firstName("Bob").lastName("B")
                .institution("NIT").department("ME").graduationYear((short) 2025).build());

        var job = saveOpenJob();
        var resumeB = saveResume(profileB);
        applicationRepository.save(Application.builder()
                .student(profileB).job(job).resume(resumeB).status(ApplicationStatus.APPLIED).build());

        mockMvc.perform(get("/api/v1/students/applications")
                        .header("Authorization", bearerHeader(studentUser)))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String body = result.getResponse().getContentAsString();
                    org.assertj.core.api.Assertions.assertThat(body).contains("\"totalElements\":0");
                });
    }
}
