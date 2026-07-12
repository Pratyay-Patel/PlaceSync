package com.placesync.application.repository;

import com.placesync.application.entity.Application;
import com.placesync.application.entity.ApplicationStatus;
import com.placesync.company.entity.Company;
import com.placesync.company.entity.CompanyStatus;
import com.placesync.company.repository.CompanyRepository;
import com.placesync.job.entity.Job;
import com.placesync.job.entity.JobLocationType;
import com.placesync.job.entity.JobStatus;
import com.placesync.job.entity.JobType;
import com.placesync.job.repository.JobRepository;
import com.placesync.recruiter.entity.RecruiterProfile;
import com.placesync.recruiter.repository.RecruiterProfileRepository;
import com.placesync.user.entity.Resume;
import com.placesync.user.entity.StudentProfile;
import com.placesync.user.entity.User;
import com.placesync.user.entity.UserRole;
import com.placesync.user.repository.ResumeRepository;
import com.placesync.user.repository.StudentProfileRepository;
import com.placesync.user.repository.UserRepository;
import com.placesync.common.SharedPostgresContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
class ApplicationRepositoryTest {

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

    @Autowired ApplicationRepository applicationRepository;
    @Autowired UserRepository userRepository;
    @Autowired StudentProfileRepository studentProfileRepository;
    @Autowired CompanyRepository companyRepository;
    @Autowired RecruiterProfileRepository recruiterProfileRepository;
    @Autowired JobRepository jobRepository;
    @Autowired ResumeRepository resumeRepository;

    StudentProfile student;
    Job job;
    Resume resume;

    @BeforeEach
    void setUp() {
        User studentUser = userRepository.save(User.builder()
                .email("student@test.com").passwordHash("hash").role(UserRole.ROLE_STUDENT).build());
        student = studentProfileRepository.save(StudentProfile.builder()
                .user(studentUser).firstName("Jane").lastName("Doe")
                .institution("IIT").department("CS").graduationYear((short) 2026).build());
        resume = resumeRepository.save(Resume.builder()
                .student(student).label("CV").originalFilename("cv.pdf")
                .s3Key("test/cv.pdf").fileSizeBytes(2048L).build());

        User recruiterUser = userRepository.save(User.builder()
                .email("recruiter@test.com").passwordHash("hash").role(UserRole.ROLE_RECRUITER).build());
        Company company = companyRepository.save(Company.builder()
                .name("TestCo").createdBy(recruiterUser).status(CompanyStatus.VERIFIED).build());
        RecruiterProfile recruiter = recruiterProfileRepository.save(RecruiterProfile.builder()
                .user(recruiterUser).firstName("Bob").lastName("Smith").company(company).build());
        job = jobRepository.save(Job.builder()
                .recruiter(recruiter).company(company)
                .title("Dev").description("Code")
                .locationType(JobLocationType.REMOTE).jobType(JobType.FULL_TIME)
                .applicationDeadline(OffsetDateTime.now().plusDays(30))
                .status(JobStatus.OPEN)
                .build());
    }

    private Application saveApplication() {
        return applicationRepository.save(Application.builder()
                .student(student).job(job).resume(resume).status(ApplicationStatus.APPLIED).build());
    }

    @Test
    void findByStudentIdAndJobId_existingApplication_returnsOptional() {
        saveApplication();

        Optional<Application> result = applicationRepository.findByStudentIdAndJobId(
                student.getId(), job.getId());

        assertThat(result).isPresent();
    }

    @Test
    void existsByStudentIdAndJobId_existing_returnsTrue() {
        saveApplication();

        assertThat(applicationRepository.existsByStudentIdAndJobId(student.getId(), job.getId())).isTrue();
    }

    @Test
    void existsByStudentIdAndJobId_absent_returnsFalse() {
        assertThat(applicationRepository.existsByStudentIdAndJobId(student.getId(), job.getId())).isFalse();
    }

    @Test
    void findByStudentId_returnsPagedResults() {
        saveApplication();

        Page<Application> result = applicationRepository.findByStudentId(student.getId(), PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void findByJobIdAndStatus_filtersCorrectly() {
        Application app = saveApplication();
        app.setStatus(ApplicationStatus.SHORTLISTED);
        applicationRepository.save(app);

        Page<Application> shortlisted = applicationRepository.findByJobIdAndStatus(
                job.getId(), ApplicationStatus.SHORTLISTED, PageRequest.of(0, 10));
        Page<Application> applied = applicationRepository.findByJobIdAndStatus(
                job.getId(), ApplicationStatus.APPLIED, PageRequest.of(0, 10));

        assertThat(shortlisted.getTotalElements()).isEqualTo(1);
        assertThat(applied.getTotalElements()).isZero();
    }
}
