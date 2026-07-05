package com.placesync.company.service;

import com.placesync.common.exception.ConflictException;
import com.placesync.common.exception.ResourceNotFoundException;
import com.placesync.common.storage.FileValidationService;
import com.placesync.common.storage.S3StorageService;
import com.placesync.company.dto.CompanyResponse;
import com.placesync.company.dto.CompanyVerificationRequest;
import com.placesync.company.dto.CreateCompanyRequest;
import com.placesync.company.dto.UpdateCompanyRequest;
import com.placesync.company.entity.Company;
import com.placesync.company.entity.CompanyStatus;
import com.placesync.company.mapper.CompanyMapper;
import com.placesync.company.repository.CompanyRepository;
import com.placesync.recruiter.repository.RecruiterProfileRepository;
import com.placesync.user.entity.User;
import com.placesync.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock CompanyRepository companyRepository;
    @Mock CompanyMapper companyMapper;
    @Mock UserRepository userRepository;
    @Mock RecruiterProfileRepository recruiterProfileRepository;
    @Mock S3StorageService s3StorageService;
    @Mock FileValidationService fileValidationService;

    @InjectMocks CompanyService companyService;

    private final UUID userId = UUID.randomUUID();
    private final UUID companyId = UUID.randomUUID();

    private User user() {
        User u = new User();
        u.setId(userId);
        return u;
    }

    private Company pendingCompany(User creator) {
        Company c = new Company();
        c.setId(companyId);
        c.setName("Acme");
        c.setStatus(CompanyStatus.PENDING_VERIFICATION);
        c.setCreatedBy(creator);
        return c;
    }

    @Test
    void createCompany_uniqueName_savesAndReturns() {
        CreateCompanyRequest req = new CreateCompanyRequest();
        req.setName("NewCo");
        when(companyRepository.existsByNameAndDeletedAtIsNull("NewCo")).thenReturn(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user()));
        when(companyRepository.save(any())).thenReturn(new Company());
        when(companyMapper.toResponse(any())).thenReturn(CompanyResponse.builder().build());

        companyService.createCompany(userId, req);

        verify(companyRepository).save(any(Company.class));
    }

    @Test
    void createCompany_duplicateName_throwsConflictException() {
        CreateCompanyRequest req = new CreateCompanyRequest();
        req.setName("Existing");
        when(companyRepository.existsByNameAndDeletedAtIsNull("Existing")).thenReturn(true);

        assertThatThrownBy(() -> companyService.createCompany(userId, req))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void updateCompany_byCreator_updatesSuccessfully() {
        User creator = user();
        Company company = pendingCompany(creator);
        UpdateCompanyRequest req = new UpdateCompanyRequest();
        req.setName("Acme");
        req.setDescription("Updated");
        when(companyRepository.findByIdAndDeletedAtIsNull(companyId)).thenReturn(Optional.of(company));
        when(companyRepository.save(company)).thenReturn(company);
        when(companyMapper.toResponse(company)).thenReturn(CompanyResponse.builder().build());

        companyService.updateCompany(userId, companyId, req);

        verify(companyRepository).save(company);
    }

    @Test
    void updateCompany_byNonCreator_throwsAccessDeniedException() {
        User creator = user();
        Company company = pendingCompany(creator);
        UUID otherUserId = UUID.randomUUID();
        when(companyRepository.findByIdAndDeletedAtIsNull(companyId)).thenReturn(Optional.of(company));

        UpdateCompanyRequest companyReq = new UpdateCompanyRequest();
        assertThatThrownBy(() -> companyService.updateCompany(otherUserId, companyId, companyReq))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void softDeleteCompany_byCreator_setsDeletedAt() {
        User creator = user();
        Company company = pendingCompany(creator);
        when(companyRepository.findByIdAndDeletedAtIsNull(companyId)).thenReturn(Optional.of(company));

        companyService.softDeleteCompany(userId, companyId);

        assertThat(company.getDeletedAt()).isNotNull();
        assertThat(company.getStatus()).isEqualTo(CompanyStatus.DEACTIVATED);
    }

    @Test
    void softDeleteCompany_byNonCreator_throwsAccessDeniedException() {
        User creator = user();
        Company company = pendingCompany(creator);
        when(companyRepository.findByIdAndDeletedAtIsNull(companyId)).thenReturn(Optional.of(company));

        UUID randomId = UUID.randomUUID();
        assertThatThrownBy(() -> companyService.softDeleteCompany(randomId, companyId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void processVerification_approve_setsVerifiedStatus() {
        User creator = user();
        Company company = pendingCompany(creator);
        CompanyVerificationRequest req = new CompanyVerificationRequest();
        req.setDecision(CompanyVerificationRequest.Decision.APPROVE);
        when(companyRepository.findByIdAndDeletedAtIsNull(companyId)).thenReturn(Optional.of(company));
        when(userRepository.findById(userId)).thenReturn(Optional.of(creator));
        when(companyRepository.save(company)).thenReturn(company);
        when(companyMapper.toResponse(company)).thenReturn(CompanyResponse.builder().build());

        companyService.processVerification(userId, companyId, req);

        assertThat(company.getStatus()).isEqualTo(CompanyStatus.VERIFIED);
    }

    @Test
    void processVerification_reject_requiresReason() {
        User creator = user();
        Company company = pendingCompany(creator);
        CompanyVerificationRequest req = new CompanyVerificationRequest();
        req.setDecision(CompanyVerificationRequest.Decision.REJECT);
        when(companyRepository.findByIdAndDeletedAtIsNull(companyId)).thenReturn(Optional.of(company));
        when(userRepository.findById(userId)).thenReturn(Optional.of(creator));

        assertThatThrownBy(() -> companyService.processVerification(userId, companyId, req))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void processVerification_alreadyVerified_throwsConflictException() {
        User creator = user();
        Company company = pendingCompany(creator);
        company.setStatus(CompanyStatus.VERIFIED);
        CompanyVerificationRequest req = new CompanyVerificationRequest();
        req.setDecision(CompanyVerificationRequest.Decision.APPROVE);
        when(companyRepository.findByIdAndDeletedAtIsNull(companyId)).thenReturn(Optional.of(company));

        assertThatThrownBy(() -> companyService.processVerification(userId, companyId, req))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void getCompany_notFound_throwsResourceNotFoundException() {
        when(companyRepository.findByIdAndDeletedAtIsNull(companyId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> companyService.getCompany(companyId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void uploadLogo_creatorUploads_savesKeyAndReturnsLogoUrl() {
        User creator = user();
        Company company = pendingCompany(creator);
        byte[] pngHeader = {(byte) 0x89, 0x50, 0x4E, 0x47, 0, 0, 0, 0};
        MockMultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", pngHeader);
        CompanyResponse base = CompanyResponse.builder().id(companyId).name("Acme").build();

        when(companyRepository.findByIdAndDeletedAtIsNull(companyId)).thenReturn(Optional.of(company));
        when(companyRepository.save(any())).thenReturn(company);
        when(companyMapper.toResponse(company)).thenReturn(base);
        when(s3StorageService.generatePresignedGetUrl(any(), any(), anyInt())).thenReturn("https://s3.example.com/logo");

        CompanyResponse result = companyService.uploadLogo(userId, companyId, file);

        verify(fileValidationService).validateLogoImage(file);
        verify(s3StorageService).uploadFile(any(), any(), any(), any(), anyLong());
        assertThat(result.getLogoUrl()).isEqualTo("https://s3.example.com/logo");
    }

    @Test
    void uploadLogo_notCreator_throwsAccessDeniedException() {
        User other = new User();
        other.setId(UUID.randomUUID());
        Company company = pendingCompany(other);
        MockMultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", new byte[]{});

        when(companyRepository.findByIdAndDeletedAtIsNull(companyId)).thenReturn(Optional.of(company));

        assertThatThrownBy(() -> companyService.uploadLogo(userId, companyId, file))
                .isInstanceOf(AccessDeniedException.class);
    }
}
