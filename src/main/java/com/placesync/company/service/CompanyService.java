package com.placesync.company.service;

import com.placesync.common.audit.AuditAction;
import com.placesync.common.audit.Auditable;
import com.placesync.common.exception.ConflictException;
import com.placesync.common.exception.ResourceNotFoundException;
import com.placesync.common.util.PagedResponse;
import com.placesync.company.dto.CompanyResponse;
import com.placesync.company.dto.CompanyVerificationRequest;
import com.placesync.company.mapper.CompanyMapper;
import com.placesync.company.dto.CreateCompanyRequest;
import com.placesync.company.dto.UpdateCompanyRequest;
import com.placesync.company.entity.Company;
import com.placesync.company.entity.CompanyStatus;
import com.placesync.company.repository.CompanyRepository;
import com.placesync.recruiter.entity.RecruiterProfile;
import com.placesync.recruiter.repository.RecruiterProfileRepository;
import com.placesync.user.entity.User;
import com.placesync.user.repository.UserRepository;
import static com.placesync.common.util.LogSanitizer.sanitize;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private static final Logger log = LoggerFactory.getLogger(CompanyService.class);
    private static final String COMPANY = "Company";

    private final CompanyRepository companyRepository;
    private final CompanyMapper companyMapper;
    private final UserRepository userRepository;
    private final RecruiterProfileRepository recruiterProfileRepository;

    @Transactional(readOnly = true)
    public CompanyResponse getCompany(UUID companyId) {
        Company company = companyRepository.findByIdAndDeletedAtIsNull(companyId)
                .orElseThrow(() -> new ResourceNotFoundException(COMPANY, companyId));
        return companyMapper.toResponse(company);
    }

    @Transactional(readOnly = true)
    public PagedResponse<CompanyResponse> getVerifiedCompanies(Pageable pageable) {
        Page<Company> page = companyRepository.findByStatusAndDeletedAtIsNull(CompanyStatus.VERIFIED, pageable);
        return PagedResponse.of(page.map(companyMapper::toResponse));
    }

    @Auditable(action = AuditAction.CREATE, entityType = "Company")
    @Transactional
    public CompanyResponse createCompany(UUID userId, CreateCompanyRequest req) {
        log.info("Creating company for userId={}", sanitize(userId));
        if (companyRepository.existsByNameAndDeletedAtIsNull(req.getName())) {
            throw new ConflictException("A company with this name already exists: " + req.getName());
        }

        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Company company = Company.builder()
                .name(req.getName())
                .description(req.getDescription())
                .websiteUrl(req.getWebsiteUrl())
                .industry(req.getIndustry())
                .headquarters(req.getHeadquarters())
                .status(CompanyStatus.PENDING_VERIFICATION)
                .createdBy(creator)
                .build();

        return companyMapper.toResponse(companyRepository.save(company));
    }

    @Transactional
    public CompanyResponse updateCompany(UUID userId, UUID companyId, UpdateCompanyRequest req) {
        log.info("Updating companyId={} by userId={}", sanitize(companyId), sanitize(userId));
        Company company = companyRepository.findByIdAndDeletedAtIsNull(companyId)
                .orElseThrow(() -> new ResourceNotFoundException(COMPANY, companyId));

        if (!company.getCreatedBy().getId().equals(userId)) {
            throw new AccessDeniedException("Only the company creator can update it");
        }
        if (!company.getName().equals(req.getName())
                && companyRepository.existsByNameAndDeletedAtIsNull(req.getName())) {
            throw new ConflictException("A company with this name already exists: " + req.getName());
        }

        company.setName(req.getName());
        company.setDescription(req.getDescription());
        company.setWebsiteUrl(req.getWebsiteUrl());
        company.setIndustry(req.getIndustry());
        company.setHeadquarters(req.getHeadquarters());

        return companyMapper.toResponse(companyRepository.save(company));
    }

    @Auditable(action = AuditAction.SOFT_DELETE, entityType = "Company", entityIdParamIndex = 1)
    @Transactional
    public void softDeleteCompany(UUID userId, UUID companyId) {
        log.info("Soft-deleting companyId={} by userId={}", sanitize(companyId), sanitize(userId));
        Company company = companyRepository.findByIdAndDeletedAtIsNull(companyId)
                .orElseThrow(() -> new ResourceNotFoundException(COMPANY, companyId));

        if (!company.getCreatedBy().getId().equals(userId)) {
            throw new AccessDeniedException("Only the company creator can delete it");
        }

        company.setDeletedAt(OffsetDateTime.now());
        company.setStatus(CompanyStatus.DEACTIVATED);
        companyRepository.save(company);
    }

    // ── Admin endpoints ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PagedResponse<CompanyResponse> getPendingCompanies(Pageable pageable) {
        Page<Company> page = companyRepository.findByStatusAndDeletedAtIsNull(
                CompanyStatus.PENDING_VERIFICATION, pageable);
        return PagedResponse.of(page.map(companyMapper::toResponse));
    }

    @Auditable(action = AuditAction.UPDATE, entityType = "Company")

    @Transactional
    public CompanyResponse processVerification(UUID adminUserId, UUID companyId,
                                               CompanyVerificationRequest req) {
        log.info("Processing company verification: companyId={}, decision={}, adminUserId={}",
                sanitize(companyId), sanitize(req.getDecision()), sanitize(adminUserId));
        Company company = companyRepository.findByIdAndDeletedAtIsNull(companyId)
                .orElseThrow(() -> new ResourceNotFoundException(COMPANY, companyId));

        if (company.getStatus() != CompanyStatus.PENDING_VERIFICATION) {
            throw new ConflictException("Company is not in PENDING_VERIFICATION state");
        }

        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", adminUserId));

        if (req.getDecision() == CompanyVerificationRequest.Decision.APPROVE) {
            company.setStatus(CompanyStatus.VERIFIED);
            company.setVerifiedBy(admin);
            company.setVerifiedAt(OffsetDateTime.now());
        } else {
            if (req.getRejectionReason() == null || req.getRejectionReason().isBlank()) {
                throw new IllegalArgumentException("rejectionReason is required when rejecting");
            }
            company.setStatus(CompanyStatus.REJECTED);
            company.setVerifiedBy(admin);
            company.setVerifiedAt(OffsetDateTime.now());
        }

        return companyMapper.toResponse(companyRepository.save(company));
    }
}
