package com.placesync.analytics.service;

import com.placesync.application.entity.ApplicationStatus;
import com.placesync.application.repository.ApplicationRepository;
import com.placesync.common.event.ApplicationStatusChangedEvent;
import com.placesync.common.event.OfferReleasedEvent;
import com.placesync.common.exception.ResourceNotFoundException;
import com.placesync.company.repository.CompanyRepository;
import com.placesync.job.entity.JobStatus;
import com.placesync.job.repository.JobRepository;
import com.placesync.recruiter.entity.RecruiterProfile;
import com.placesync.recruiter.repository.RecruiterProfileRepository;
import com.placesync.user.entity.UserRole;
import com.placesync.user.repository.StudentProfileRepository;
import com.placesync.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private StudentProfileRepository studentProfileRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private JobRepository jobRepository;
    @Mock private ApplicationRepository applicationRepository;
    @Mock private RecruiterProfileRepository recruiterProfileRepository;
    @Mock private CacheManager cacheManager;

    @InjectMocks
    private AnalyticsService analyticsService;

    @Test
    void getPlacementStats_validData_returnsCorrectStats() {
        when(userRepository.countByRole(UserRole.ROLE_STUDENT)).thenReturn(100L);
        when(userRepository.countByRole(UserRole.ROLE_RECRUITER)).thenReturn(20L);
        when(companyRepository.countByDeletedAtIsNull()).thenReturn(10L);
        when(jobRepository.countByStatusAndDeletedAtIsNull(JobStatus.OPEN)).thenReturn(15L);
        when(applicationRepository.count()).thenReturn(200L);
        when(applicationRepository.countByStatus(ApplicationStatus.OFFERED)).thenReturn(40L);

        var result = analyticsService.getPlacementStats();

        assertThat(result.totalStudents()).isEqualTo(100L);
        assertThat(result.totalRecruiters()).isEqualTo(20L);
        assertThat(result.totalCompanies()).isEqualTo(10L);
        assertThat(result.openJobs()).isEqualTo(15L);
        assertThat(result.totalApplications()).isEqualTo(200L);
        assertThat(result.totalOffers()).isEqualTo(40L);
        assertThat(result.placementRate()).isEqualTo(40.0);
    }

    @Test
    void getPlacementStats_noStudents_returnsZeroPlacementRate() {
        when(userRepository.countByRole(UserRole.ROLE_STUDENT)).thenReturn(0L);
        when(userRepository.countByRole(UserRole.ROLE_RECRUITER)).thenReturn(0L);
        when(companyRepository.countByDeletedAtIsNull()).thenReturn(0L);
        when(jobRepository.countByStatusAndDeletedAtIsNull(JobStatus.OPEN)).thenReturn(0L);
        when(applicationRepository.count()).thenReturn(0L);
        when(applicationRepository.countByStatus(ApplicationStatus.OFFERED)).thenReturn(0L);

        var result = analyticsService.getPlacementStats();

        assertThat(result.placementRate()).isEqualTo(0.0);
    }

    @Test
    void getCompanyBreakdown_returnsPopulatedList() {
        UUID companyId = UUID.randomUUID();
        Object[] row = new Object[]{companyId.toString(), "Acme Corp", 5L, 3L, 20L};
        when(applicationRepository.findCompanyBreakdown()).thenReturn(List.<Object[]>of(row));

        var result = analyticsService.getCompanyBreakdown();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).companyName()).isEqualTo("Acme Corp");
        assertThat(result.get(0).offerCount()).isEqualTo(5L);
    }

    @Test
    void getCompanyBreakdown_emptyData_returnsEmptyList() {
        when(applicationRepository.findCompanyBreakdown()).thenReturn(List.of());

        var result = analyticsService.getCompanyBreakdown();

        assertThat(result).isEmpty();
    }

    @Test
    void getDepartmentBreakdown_mergesStudentsAndPlacements() {
        Object[] deptRow = new Object[]{"Computer Science", 50L};
        Object[] placedRow = new Object[]{"Computer Science", 10L};
        when(studentProfileRepository.countStudentsByDepartment()).thenReturn(List.<Object[]>of(deptRow));
        when(applicationRepository.countOfferedByDepartment()).thenReturn(List.<Object[]>of(placedRow));

        var result = analyticsService.getDepartmentBreakdown();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).department()).isEqualTo("Computer Science");
        assertThat(result.get(0).totalStudents()).isEqualTo(50L);
        assertThat(result.get(0).placedCount()).isEqualTo(10L);
        assertThat(result.get(0).placementRate()).isEqualTo(20.0);
    }

    @Test
    void getDepartmentBreakdown_departmentWithNoOffers_returnsZeroRate() {
        Object[] deptRow = new Object[]{"Mechanical", 30L};
        when(studentProfileRepository.countStudentsByDepartment()).thenReturn(List.<Object[]>of(deptRow));
        when(applicationRepository.countOfferedByDepartment()).thenReturn(List.<Object[]>of());

        var result = analyticsService.getDepartmentBreakdown();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).placedCount()).isEqualTo(0L);
        assertThat(result.get(0).placementRate()).isEqualTo(0.0);
    }

    @Test
    void getRecruiterStats_validUserId_returnsStats() {
        UUID userId = UUID.randomUUID();
        UUID recruiterId = UUID.randomUUID();
        RecruiterProfile profile = RecruiterProfile.builder().id(recruiterId).build();
        Object[] stats = new Object[]{50L, 10L, 5L};

        when(recruiterProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(jobRepository.countByRecruiterIdAndDeletedAtIsNull(recruiterId)).thenReturn(8L);
        when(applicationRepository.findRecruiterApplicationStats(recruiterId)).thenReturn(stats);

        var result = analyticsService.getRecruiterStats(userId);

        assertThat(result.jobsPosted()).isEqualTo(8L);
        assertThat(result.totalApplications()).isEqualTo(50L);
        assertThat(result.shortlisted()).isEqualTo(10L);
        assertThat(result.offers()).isEqualTo(5L);
    }

    @Test
    void getRecruiterStats_nullStatsRow_returnsZeros() {
        UUID userId = UUID.randomUUID();
        UUID recruiterId = UUID.randomUUID();
        RecruiterProfile profile = RecruiterProfile.builder().id(recruiterId).build();
        Object[] stats = new Object[]{null, null, null};

        when(recruiterProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(jobRepository.countByRecruiterIdAndDeletedAtIsNull(recruiterId)).thenReturn(0L);
        when(applicationRepository.findRecruiterApplicationStats(recruiterId)).thenReturn(stats);

        var result = analyticsService.getRecruiterStats(userId);

        assertThat(result.totalApplications()).isEqualTo(0L);
        assertThat(result.shortlisted()).isEqualTo(0L);
        assertThat(result.offers()).isEqualTo(0L);
    }

    @Test
    void getRecruiterStats_noProfile_throwsResourceNotFoundException() {
        UUID userId = UUID.randomUUID();
        when(recruiterProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> analyticsService.getRecruiterStats(userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Recruiter profile not found");
    }

    @Test
    void onOfferReleased_evictsAnalyticsDashboardCache() {
        Cache mockCache = mock(Cache.class);
        when(cacheManager.getCache("analytics-dashboard")).thenReturn(mockCache);

        analyticsService.onOfferReleased(OfferReleasedEvent.of(
                UUID.randomUUID(), UUID.randomUUID(), "SWE", "Acme"));

        verify(mockCache).clear();
    }

    @Test
    void onApplicationStatusChanged_evictsRecruiterAnalyticsCache() {
        Cache mockCache = mock(Cache.class);
        when(cacheManager.getCache("recruiter-analytics")).thenReturn(mockCache);

        analyticsService.onApplicationStatusChanged(ApplicationStatusChangedEvent.of(
                UUID.randomUUID(), UUID.randomUUID(),
                ApplicationStatus.APPLIED, ApplicationStatus.SHORTLISTED));

        verify(mockCache).clear();
    }

    @Test
    void onOfferReleased_nullCache_doesNotThrow() {
        when(cacheManager.getCache("analytics-dashboard")).thenReturn(null);
        OfferReleasedEvent event = OfferReleasedEvent.of(UUID.randomUUID(), UUID.randomUUID(), "SWE", "Acme");

        assertThatNoException().isThrownBy(() -> analyticsService.onOfferReleased(event));
    }

    @Test
    void onApplicationStatusChanged_nullCache_doesNotThrow() {
        when(cacheManager.getCache("recruiter-analytics")).thenReturn(null);
        ApplicationStatusChangedEvent event = ApplicationStatusChangedEvent.of(
                UUID.randomUUID(), UUID.randomUUID(),
                ApplicationStatus.APPLIED, ApplicationStatus.SHORTLISTED);

        assertThatNoException().isThrownBy(() -> analyticsService.onApplicationStatusChanged(event));
    }
}
