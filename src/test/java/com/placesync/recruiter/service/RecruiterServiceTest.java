package com.placesync.recruiter.service;

import com.placesync.auth.service.EmailService;
import com.placesync.common.exception.ConflictException;
import com.placesync.common.exception.ResourceNotFoundException;
import com.placesync.common.kafka.KafkaEventPublisher;
import com.placesync.company.entity.Company;
import com.placesync.company.repository.CompanyRepository;
import com.placesync.recruiter.dto.RecruiterProfileResponse;
import com.placesync.recruiter.dto.RecruiterVerificationRequest;
import com.placesync.recruiter.dto.UpdateRecruiterProfileRequest;
import com.placesync.recruiter.entity.RecruiterProfile;
import com.placesync.recruiter.entity.VerificationStatus;
import com.placesync.recruiter.mapper.RecruiterMapper;
import com.placesync.recruiter.repository.RecruiterProfileRepository;
import com.placesync.user.entity.User;
import com.placesync.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecruiterServiceTest {

    @Mock RecruiterProfileRepository recruiterProfileRepository;
    @Mock RecruiterMapper recruiterMapper;
    @Mock CompanyRepository companyRepository;
    @Mock UserRepository userRepository;
    @Mock EmailService emailService;
    @Mock KafkaEventPublisher kafkaEventPublisher;

    @InjectMocks RecruiterService recruiterService;

    private final UUID userId = UUID.randomUUID();
    private final UUID recruiterId = UUID.randomUUID();
    private final UUID adminId = UUID.randomUUID();

    private RecruiterProfile pendingProfile() {
        User user = new User();
        user.setId(userId);
        user.setEmail("recruiter@test.com");
        RecruiterProfile p = new RecruiterProfile();
        p.setId(recruiterId);
        p.setUser(user);
        p.setFirstName("John");
        p.setLastName("Doe");
        p.setVerificationStatus(VerificationStatus.PENDING_VERIFICATION);
        return p;
    }

    @Test
    void getMyProfile_found_returnsMappedResponse() {
        RecruiterProfile profile = pendingProfile();
        when(recruiterProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(recruiterMapper.toResponse(profile)).thenReturn(RecruiterProfileResponse.builder().build());

        recruiterService.getMyProfile(userId);

        verify(recruiterMapper).toResponse(profile);
    }

    @Test
    void getMyProfile_notFound_throwsResourceNotFoundException() {
        when(recruiterProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recruiterService.getMyProfile(userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateProfile_updatesBasicFields() {
        RecruiterProfile profile = pendingProfile();
        UpdateRecruiterProfileRequest req = new UpdateRecruiterProfileRequest();
        req.setFirstName("Jane");
        req.setLastName("Smith");
        when(recruiterProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(recruiterProfileRepository.save(profile)).thenReturn(profile);
        when(recruiterMapper.toResponse(profile)).thenReturn(RecruiterProfileResponse.builder().build());

        recruiterService.updateProfile(userId, req);

        assertThat(profile.getFirstName()).isEqualTo("Jane");
    }

    @Test
    void updateProfile_withCompany_setsCompanyOnProfile() {
        RecruiterProfile profile = pendingProfile();
        UUID companyId = UUID.randomUUID();
        Company company = new Company();
        UpdateRecruiterProfileRequest req = new UpdateRecruiterProfileRequest();
        req.setFirstName("Jane");
        req.setLastName("Smith");
        req.setCompanyId(companyId);
        when(recruiterProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(companyRepository.findByIdAndDeletedAtIsNull(companyId)).thenReturn(Optional.of(company));
        when(recruiterProfileRepository.save(profile)).thenReturn(profile);
        when(recruiterMapper.toResponse(profile)).thenReturn(RecruiterProfileResponse.builder().build());

        recruiterService.updateProfile(userId, req);

        assertThat(profile.getCompany()).isEqualTo(company);
    }

    @Test
    void processVerification_approve_setsVerifiedStatus() {
        RecruiterProfile profile = pendingProfile();
        User admin = new User();
        admin.setId(adminId);
        RecruiterVerificationRequest req = new RecruiterVerificationRequest();
        req.setDecision(RecruiterVerificationRequest.Decision.APPROVE);
        when(recruiterProfileRepository.findById(recruiterId)).thenReturn(Optional.of(profile));
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(recruiterProfileRepository.save(profile)).thenReturn(profile);
        when(recruiterMapper.toResponse(profile)).thenReturn(RecruiterProfileResponse.builder().build());

        recruiterService.processVerification(adminId, recruiterId, req);

        assertThat(profile.getVerificationStatus()).isEqualTo(VerificationStatus.VERIFIED);
        verify(emailService).sendRecruiterApprovedEmail(any(), any());
        verify(kafkaEventPublisher).publish(any());
    }

    @Test
    void processVerification_reject_requiresReason() {
        RecruiterProfile profile = pendingProfile();
        User admin = new User();
        admin.setId(adminId);
        RecruiterVerificationRequest req = new RecruiterVerificationRequest();
        req.setDecision(RecruiterVerificationRequest.Decision.REJECT);
        when(recruiterProfileRepository.findById(recruiterId)).thenReturn(Optional.of(profile));
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> recruiterService.processVerification(adminId, recruiterId, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rejectionReason");
    }

    @Test
    void processVerification_reject_withReason_setsRejectedStatus() {
        RecruiterProfile profile = pendingProfile();
        User admin = new User();
        admin.setId(adminId);
        RecruiterVerificationRequest req = new RecruiterVerificationRequest();
        req.setDecision(RecruiterVerificationRequest.Decision.REJECT);
        req.setRejectionReason("Incomplete information");
        when(recruiterProfileRepository.findById(recruiterId)).thenReturn(Optional.of(profile));
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(recruiterProfileRepository.save(profile)).thenReturn(profile);
        when(recruiterMapper.toResponse(profile)).thenReturn(RecruiterProfileResponse.builder().build());

        recruiterService.processVerification(adminId, recruiterId, req);

        assertThat(profile.getVerificationStatus()).isEqualTo(VerificationStatus.REJECTED);
        verify(emailService).sendRecruiterRejectedEmail(any(), any(), any());
    }

    @Test
    void processVerification_alreadyVerified_throwsConflictException() {
        RecruiterProfile profile = pendingProfile();
        profile.setVerificationStatus(VerificationStatus.VERIFIED);
        RecruiterVerificationRequest req = new RecruiterVerificationRequest();
        req.setDecision(RecruiterVerificationRequest.Decision.APPROVE);
        when(recruiterProfileRepository.findById(recruiterId)).thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> recruiterService.processVerification(adminId, recruiterId, req))
                .isInstanceOf(ConflictException.class);
    }
}
