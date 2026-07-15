package com.placesync.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.placesync.common.security.JwtTokenProvider;
import com.placesync.company.entity.Company;
import com.placesync.company.entity.CompanyStatus;
import com.placesync.company.repository.CompanyRepository;
import com.placesync.job.entity.*;
import com.placesync.job.repository.JobRepository;
import com.placesync.recruiter.entity.RecruiterProfile;
import com.placesync.recruiter.entity.VerificationStatus;
import com.placesync.recruiter.repository.RecruiterProfileRepository;
import com.placesync.user.entity.*;
import com.placesync.user.repository.ResumeRepository;
import com.placesync.user.repository.StudentProfileRepository;
import com.placesync.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("itest")
public abstract class AbstractIntegrationTest {

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", SharedPostgresContainer.POSTGRES::getJdbcUrl);
        r.add("spring.datasource.username", SharedPostgresContainer.POSTGRES::getUsername);
        r.add("spring.datasource.password", SharedPostgresContainer.POSTGRES::getPassword);
        r.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        r.add("spring.flyway.enabled", () -> "true");
        r.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        r.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper objectMapper;
    @Autowired protected JwtTokenProvider jwtTokenProvider;
    @Autowired protected UserRepository userRepository;
    @Autowired protected StudentProfileRepository studentProfileRepository;
    @Autowired protected RecruiterProfileRepository recruiterProfileRepository;
    @Autowired protected CompanyRepository companyRepository;
    @Autowired protected JobRepository jobRepository;
    @Autowired protected ResumeRepository resumeRepository;

    protected User studentUser;
    protected User recruiterUser;
    protected User adminUser;
    protected StudentProfile studentProfile;
    protected RecruiterProfile recruiterProfile;
    protected Company company;

    @BeforeEach
    void setUpUsers() {
        studentUser = userRepository.save(User.builder()
                .email("student@itest.com").passwordHash("hash")
                .role(UserRole.ROLE_STUDENT).build());
        studentProfile = studentProfileRepository.save(StudentProfile.builder()
                .user(studentUser).firstName("Alice").lastName("Student")
                .institution("IIT").department("CS").graduationYear((short) 2026).build());

        recruiterUser = userRepository.save(User.builder()
                .email("recruiter@itest.com").passwordHash("hash")
                .role(UserRole.ROLE_RECRUITER).build());
        company = companyRepository.save(Company.builder()
                .name("TestCo").createdBy(recruiterUser).status(CompanyStatus.VERIFIED).build());
        recruiterProfile = recruiterProfileRepository.save(RecruiterProfile.builder()
                .user(recruiterUser).firstName("Bob").lastName("Recruiter")
                .verificationStatus(VerificationStatus.VERIFIED)
                .company(company).build());

        adminUser = userRepository.save(User.builder()
                .email("admin@itest.com").passwordHash("hash")
                .role(UserRole.ROLE_ADMIN).build());
    }

    protected String bearerHeader(User user) {
        String token = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        return "Bearer " + token;
    }

    protected Job saveOpenJob() {
        return jobRepository.save(Job.builder()
                .recruiter(recruiterProfile)
                .company(company)
                .title("Software Engineer")
                .description("Write code")
                .locationType(JobLocationType.REMOTE)
                .jobType(JobType.FULL_TIME)
                .applicationDeadline(OffsetDateTime.now().plusDays(30))
                .status(JobStatus.OPEN)
                .build());
    }

    protected Resume saveResume(StudentProfile student) {
        return resumeRepository.save(Resume.builder()
                .student(student).label("CV")
                .originalFilename("cv.pdf").s3Key("test/cv.pdf").fileSizeBytes(1024L)
                .build());
    }
}
