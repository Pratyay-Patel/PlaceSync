package com.placesync.recruiter.service;

import com.placesync.auth.service.EmailService;
import com.placesync.common.audit.AuditAction;
import com.placesync.common.audit.Auditable;
import com.placesync.common.event.RecruiterVerifiedEvent;
import com.placesync.common.exception.ConflictException;
import com.placesync.common.exception.ResourceNotFoundException;
import com.placesync.common.kafka.KafkaEventPublisher;
import com.placesync.common.util.PagedResponse;
import com.placesync.company.entity.Company;
import com.placesync.company.repository.CompanyRepository;
import com.placesync.recruiter.dto.RecruiterProfileResponse;
import com.placesync.recruiter.dto.RecruiterVerificationRequest;
import com.placesync.recruiter.dto.UpdateRecruiterProfileRequest;
import com.placesync.recruiter.mapper.RecruiterMapper;
import com.placesync.recruiter.entity.RecruiterProfile;
import com.placesync.recruiter.entity.VerificationStatus;
import com.placesync.recruiter.repository.RecruiterProfileRepository;
import com.placesync.user.entity.User;
import com.placesync.user.repository.UserRepository;
import static com.placesync.common.util.LogSanitizer.sanitize;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@SuppressWarnings("java:S2629")
@Service
@RequiredArgsConstructor
public class RecruiterService {

    private static final Logger log = LoggerFactory.getLogger(RecruiterService.class);
    private static final String RECRUITER_PROFILE = "RecruiterProfile";

    private final RecruiterProfileRepository recruiterProfileRepository;
    private final RecruiterMapper recruiterMapper;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final KafkaEventPublisher kafkaEventPublisher;

    @Transactional(readOnly = true)
    public RecruiterProfileResponse getMyProfile(UUID userId) {
        RecruiterProfile profile = recruiterProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(RECRUITER_PROFILE, userId));
        return recruiterMapper.toResponse(profile);
    }

    @Auditable(action = AuditAction.UPDATE, entityType = "RecruiterProfile")
    @Transactional
    public RecruiterProfileResponse updateProfile(UUID userId, UpdateRecruiterProfileRequest req) {
        log.info("Updating recruiter profile for userId={}", sanitize(userId));
        RecruiterProfile profile = recruiterProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(RECRUITER_PROFILE, userId));

        profile.setFirstName(req.getFirstName());
        profile.setLastName(req.getLastName());
        profile.setJobTitle(req.getJobTitle());
        profile.setContactEmail(req.getContactEmail());
        profile.setPhone(req.getPhone());

        if (req.getCompanyId() != null) {
            Company company = companyRepository.findByIdAndDeletedAtIsNull(req.getCompanyId())
                    .orElseThrow(() -> new ResourceNotFoundException("Company", req.getCompanyId()));
            profile.setCompany(company);
        }

        return recruiterMapper.toResponse(recruiterProfileRepository.save(profile));
    }

    @Transactional(readOnly = true)
    public PagedResponse<RecruiterProfileResponse> getPendingVerifications(Pageable pageable) {
        Page<RecruiterProfile> page = recruiterProfileRepository
                .findByVerificationStatus(VerificationStatus.PENDING_VERIFICATION, pageable);
        return PagedResponse.of(page.map(recruiterMapper::toResponse));
    }

    @Auditable(action = AuditAction.UPDATE, entityType = "RecruiterProfile")
    @Transactional
    public RecruiterProfileResponse processVerification(UUID adminUserId, UUID recruiterId,
                                                        RecruiterVerificationRequest req) {
        log.info("Processing recruiter verification: recruiterId={}, decision={}, adminUserId={}",
                sanitize(recruiterId), sanitize(req.getDecision()), sanitize(adminUserId));
        RecruiterProfile profile = recruiterProfileRepository.findById(recruiterId)
                .orElseThrow(() -> new ResourceNotFoundException(RECRUITER_PROFILE, recruiterId));

        if (profile.getVerificationStatus() != VerificationStatus.PENDING_VERIFICATION) {
            throw new ConflictException("Recruiter is not in PENDING_VERIFICATION state");
        }

        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", adminUserId));

        if (req.getDecision() == RecruiterVerificationRequest.Decision.APPROVE) {
            profile.setVerificationStatus(VerificationStatus.VERIFIED);
            profile.setVerifiedAt(OffsetDateTime.now());
            profile.setVerifiedBy(admin);
            emailService.sendRecruiterApprovedEmail(
                    profile.getUser().getEmail(),
                    profile.getFirstName() + " " + profile.getLastName());
        } else {
            if (req.getRejectionReason() == null || req.getRejectionReason().isBlank()) {
                throw new IllegalArgumentException("rejectionReason is required when rejecting");
            }
            profile.setVerificationStatus(VerificationStatus.REJECTED);
            profile.setVerifiedAt(OffsetDateTime.now());
            profile.setVerifiedBy(admin);
            profile.setRejectionReason(req.getRejectionReason());
            emailService.sendRecruiterRejectedEmail(
                    profile.getUser().getEmail(),
                    profile.getFirstName() + " " + profile.getLastName(),
                    req.getRejectionReason());
        }

        RecruiterProfile saved = recruiterProfileRepository.save(profile);
        kafkaEventPublisher.publish(RecruiterVerifiedEvent.of(
                saved.getId(), saved.getUser().getId(), req.getDecision().name()));
        return recruiterMapper.toResponse(saved);
    }
}
