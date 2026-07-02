package com.placesync.analytics.service;

import com.placesync.analytics.dto.CompanyStatsResponse;
import com.placesync.analytics.dto.DepartmentStatsResponse;
import com.placesync.analytics.dto.PlacementStatsResponse;
import com.placesync.analytics.dto.RecruiterStatsResponse;
import com.placesync.application.entity.ApplicationStatus;
import com.placesync.application.repository.ApplicationRepository;
import com.placesync.common.event.ApplicationStatusChangedEvent;
import com.placesync.common.event.OfferReleasedEvent;
import com.placesync.common.exception.ResourceNotFoundException;
import com.placesync.company.repository.CompanyRepository;
import com.placesync.job.entity.JobStatus;
import com.placesync.job.repository.JobRepository;
import com.placesync.recruiter.repository.RecruiterProfileRepository;
import com.placesync.user.entity.UserRole;
import com.placesync.user.repository.StudentProfileRepository;
import com.placesync.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import static com.placesync.common.util.LogSanitizer.sanitize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("java:S2629")
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final CompanyRepository companyRepository;
    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final RecruiterProfileRepository recruiterProfileRepository;
    private final CacheManager cacheManager;

    @Cacheable(value = "analytics-dashboard", key = "'global'")
    @Transactional(readOnly = true)
    public PlacementStatsResponse getPlacementStats() {
        log.info("Computing global placement stats");
        long students = userRepository.countByRole(UserRole.ROLE_STUDENT);
        long recruiters = userRepository.countByRole(UserRole.ROLE_RECRUITER);
        long companies = companyRepository.countByDeletedAtIsNull();
        long openJobs = jobRepository.countByStatusAndDeletedAtIsNull(JobStatus.OPEN);
        long totalApplications = applicationRepository.count();
        long totalOffers = applicationRepository.countByStatus(ApplicationStatus.OFFERED);
        double placementRate = students == 0 ? 0.0 : (double) totalOffers / students * 100;
        return new PlacementStatsResponse(students, recruiters, companies, openJobs, totalApplications, totalOffers, placementRate);
    }

    @Cacheable(value = "analytics-dashboard", key = "'company-breakdown'")
    @Transactional(readOnly = true)
    public List<CompanyStatsResponse> getCompanyBreakdown() {
        log.info("Computing company breakdown analytics");
        return applicationRepository.findCompanyBreakdown().stream()
                .map(row -> new CompanyStatsResponse(
                        UUID.fromString(row[0].toString()),
                        (String) row[1],
                        ((Number) row[2]).longValue(),
                        ((Number) row[3]).longValue(),
                        ((Number) row[4]).longValue()
                ))
                .toList();
    }

    @Cacheable(value = "analytics-dashboard", key = "'department-breakdown'")
    @Transactional(readOnly = true)
    public List<DepartmentStatsResponse> getDepartmentBreakdown() {
        log.info("Computing department breakdown analytics");

        Map<String, Long> totalByDept = new HashMap<>();
        for (Object[] row : studentProfileRepository.countStudentsByDepartment()) {
            totalByDept.put((String) row[0], ((Number) row[1]).longValue());
        }

        Map<String, Long> placedByDept = new HashMap<>();
        for (Object[] row : applicationRepository.countOfferedByDepartment()) {
            placedByDept.put((String) row[0], ((Number) row[1]).longValue());
        }

        return totalByDept.entrySet().stream()
                .map(entry -> {
                    String dept = entry.getKey();
                    long total = entry.getValue();
                    long placed = placedByDept.getOrDefault(dept, 0L);
                    double rate = total == 0 ? 0.0 : (double) placed / total * 100;
                    return new DepartmentStatsResponse(dept, placed, total, rate);
                })
                .sorted((a, b) -> Double.compare(b.placementRate(), a.placementRate()))
                .toList();
    }

    @Cacheable(value = "recruiter-analytics", key = "#userId")
    @Transactional(readOnly = true)
    public RecruiterStatsResponse getRecruiterStats(UUID userId) {
        log.info("Computing recruiter analytics for userId={}", sanitize(userId));
        var profile = recruiterProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Recruiter profile not found"));
        UUID recruiterId = profile.getId();
        long jobsPosted = jobRepository.countByRecruiterIdAndDeletedAtIsNull(recruiterId);
        Object[] stats = applicationRepository.findRecruiterApplicationStats(recruiterId);
        long total = stats[0] != null ? ((Number) stats[0]).longValue() : 0L;
        long shortlisted = stats[1] != null ? ((Number) stats[1]).longValue() : 0L;
        long offers = stats[2] != null ? ((Number) stats[2]).longValue() : 0L;
        return new RecruiterStatsResponse(jobsPosted, total, shortlisted, offers);
    }

    @EventListener
    public void onOfferReleased(OfferReleasedEvent event) {
        log.info("Evicting analytics-dashboard cache after offer released");
        var cache = cacheManager.getCache("analytics-dashboard");
        if (cache != null) {
            cache.clear();
        }
    }

    @EventListener
    public void onApplicationStatusChanged(ApplicationStatusChangedEvent event) {
        log.info("Evicting recruiter-analytics cache after application status changed");
        var cache = cacheManager.getCache("recruiter-analytics");
        if (cache != null) {
            cache.clear();
        }
    }
}
