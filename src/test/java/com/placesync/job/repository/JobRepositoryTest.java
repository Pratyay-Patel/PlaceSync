package com.placesync.job.repository;

import com.placesync.company.entity.Company;
import com.placesync.company.entity.CompanyStatus;
import com.placesync.company.repository.CompanyRepository;
import com.placesync.job.entity.Job;
import com.placesync.job.entity.JobLocationType;
import com.placesync.job.entity.JobStatus;
import com.placesync.job.entity.JobType;
import com.placesync.recruiter.entity.RecruiterProfile;
import com.placesync.recruiter.repository.RecruiterProfileRepository;
import com.placesync.user.entity.User;
import com.placesync.user.entity.UserRole;
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
class JobRepositoryTest {

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

    @Autowired JobRepository jobRepository;
    @Autowired UserRepository userRepository;
    @Autowired CompanyRepository companyRepository;
    @Autowired RecruiterProfileRepository recruiterProfileRepository;

    RecruiterProfile recruiter;
    Company company;

    @BeforeEach
    void setUp() {
        User recruiterUser = userRepository.save(User.builder()
                .email("recruiter@test.com").passwordHash("hash").role(UserRole.ROLE_RECRUITER).build());
        company = companyRepository.save(Company.builder()
                .name("TestCorp").createdBy(recruiterUser).status(CompanyStatus.VERIFIED).build());
        recruiter = recruiterProfileRepository.save(RecruiterProfile.builder()
                .user(recruiterUser).firstName("John").lastName("Doe").company(company).build());
    }

    private Job buildJob(JobStatus status) {
        return Job.builder()
                .recruiter(recruiter).company(company)
                .title("Engineer").description("Build things")
                .locationType(JobLocationType.REMOTE).jobType(JobType.FULL_TIME)
                .applicationDeadline(OffsetDateTime.now().plusDays(30))
                .status(status)
                .build();
    }

    @Test
    void findByStatusAndDeletedAtIsNull_returnsOnlyMatchingStatus() {
        jobRepository.save(buildJob(JobStatus.OPEN));
        jobRepository.save(buildJob(JobStatus.PENDING_APPROVAL));

        Page<Job> open = jobRepository.findByStatusAndDeletedAtIsNull(JobStatus.OPEN, PageRequest.of(0, 10));

        assertThat(open.getTotalElements()).isEqualTo(1);
        assertThat(open.getContent().get(0).getStatus()).isEqualTo(JobStatus.OPEN);
    }

    @Test
    void findByIdAndDeletedAtIsNull_softDeletedJob_returnsEmpty() {
        Job job = jobRepository.save(buildJob(JobStatus.OPEN));
        job.setDeletedAt(OffsetDateTime.now());
        jobRepository.save(job);

        Optional<Job> result = jobRepository.findByIdAndDeletedAtIsNull(job.getId());

        assertThat(result).isEmpty();
    }

    @Test
    void findByRecruiterIdAndDeletedAtIsNull_returnsOnlyRecruiterJobs() {
        jobRepository.save(buildJob(JobStatus.OPEN));
        jobRepository.save(buildJob(JobStatus.PENDING_APPROVAL));

        Page<Job> result = jobRepository.findByRecruiterIdAndDeletedAtIsNull(
                recruiter.getId(), PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void countByStatusAndDeletedAtIsNull_correctCount() {
        jobRepository.save(buildJob(JobStatus.OPEN));
        jobRepository.save(buildJob(JobStatus.OPEN));
        jobRepository.save(buildJob(JobStatus.CLOSED));

        long count = jobRepository.countByStatusAndDeletedAtIsNull(JobStatus.OPEN);

        assertThat(count).isEqualTo(2);
    }
}
