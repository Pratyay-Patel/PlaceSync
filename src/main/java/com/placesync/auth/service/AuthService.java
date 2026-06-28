package com.placesync.auth.service;

import com.placesync.auth.dto.*;
import com.placesync.auth.entity.EmailVerificationToken;
import com.placesync.auth.entity.PasswordResetToken;
import com.placesync.auth.entity.RefreshToken;
import com.placesync.auth.repository.EmailVerificationTokenRepository;
import com.placesync.auth.repository.PasswordResetTokenRepository;
import com.placesync.auth.repository.RefreshTokenRepository;
import com.placesync.common.audit.AuditAction;
import com.placesync.common.audit.AuditLog;
import com.placesync.common.audit.service.AuditLogService;
import com.placesync.common.config.JwtProperties;
import com.placesync.common.exception.ConflictException;
import com.placesync.common.exception.ResourceNotFoundException;
import com.placesync.common.exception.UnauthorizedException;
import com.placesync.common.security.JwtTokenProvider;
import com.placesync.common.security.UserPrincipal;
import com.placesync.recruiter.entity.RecruiterProfile;
import com.placesync.recruiter.entity.VerificationStatus;
import com.placesync.recruiter.repository.RecruiterProfileRepository;
import com.placesync.user.entity.*;
import com.placesync.user.repository.StudentProfileRepository;
import com.placesync.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final RecruiterProfileRepository recruiterProfileRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final JwtProperties jwtProperties;
    private final AuditLogService auditLogService;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (req.getRole() == UserRole.ROLE_ADMIN) {
            throw new IllegalArgumentException("Cannot self-register as ADMIN");
        }
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new ConflictException("Email already registered: " + req.getEmail());
        }
        if (req.getRole() == UserRole.ROLE_STUDENT) {
            validateStudentFields(req);
        }

        User user = User.builder()
                .email(req.getEmail())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .role(req.getRole())
                .isEmailVerified(false)
                .isActive(true)
                .failedLoginAttempts((short) 0)
                .build();
        userRepository.save(user);

        if (req.getRole() == UserRole.ROLE_STUDENT) {
            StudentProfile profile = StudentProfile.builder()
                    .user(user)
                    .firstName(req.getFirstName())
                    .lastName(req.getLastName())
                    .institution(req.getInstitution())
                    .department(req.getDepartment())
                    .graduationYear(req.getGraduationYear())
                    .isProfilePublic(true)
                    .build();
            studentProfileRepository.save(profile);
        } else if (req.getRole() == UserRole.ROLE_RECRUITER) {
            RecruiterProfile profile = RecruiterProfile.builder()
                    .user(user)
                    .firstName(req.getFirstName())
                    .lastName(req.getLastName())
                    .verificationStatus(VerificationStatus.PENDING_VERIFICATION)
                    .build();
            recruiterProfileRepository.save(profile);
        }

        String rawVerificationToken = UUID.randomUUID().toString();
        saveEmailVerificationToken(user, rawVerificationToken);
        emailService.sendEmailVerification(user.getEmail(), rawVerificationToken);

        return buildAuthResponse(user, createRefreshToken(user, UUID.randomUUID()));
    }

    @Transactional
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmailAndDeletedAtIsNull(req.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new UnauthorizedException("Account is deactivated");
        }
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(OffsetDateTime.now())) {
            throw new UnauthorizedException("Account is temporarily locked. Try again later");
        }
        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            handleFailedLogin(user);
            throw new UnauthorizedException("Invalid email or password");
        }

        resetFailedLoginAttempts(user);

        String rawRefreshToken = createRefreshToken(user, UUID.randomUUID());
        return buildAuthResponse(user, rawRefreshToken);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest req) {
        String hash = hashToken(req.getRefreshToken());

        RefreshToken stored = refreshTokenRepository.findByTokenHashAndIsRevokedFalse(hash)
                .orElseGet(() -> {
                    // Possible reuse — invalidate the entire family
                    refreshTokenRepository.findByTokenHash(hash).ifPresent(revoked -> {
                        log.warn("Refresh token reuse detected for family {}. Revoking all.", revoked.getFamilyId());
                        refreshTokenRepository.deleteByFamilyId(revoked.getFamilyId());
                    });
                    throw new UnauthorizedException("Refresh token is invalid or expired");
                });

        if (stored.getExpiresAt().isBefore(OffsetDateTime.now())) {
            revokeToken(stored);
            throw new UnauthorizedException("Refresh token has expired");
        }

        UUID familyId = stored.getFamilyId();
        revokeToken(stored);

        User user = stored.getUser();
        String newRawToken = createRefreshToken(user, familyId);
        return buildAuthResponse(user, newRawToken);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        String hash = hashToken(rawRefreshToken);
        refreshTokenRepository.findByTokenHashAndIsRevokedFalse(hash).ifPresent(this::revokeToken);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            auditLogService.saveAsync(AuditLog.builder()
                    .entityType("User").entityId(principal.getId())
                    .action(AuditAction.LOGOUT)
                    .actorId(principal.getId()).actorRole(principal.getRole().name()).actorEmail(principal.getEmail())
                    .build());
        }
    }

    @Transactional
    public void verifyEmail(String rawToken) {
        String hash = hashToken(rawToken);
        EmailVerificationToken token = emailVerificationTokenRepository
                .findByTokenHashAndIsUsedFalse(hash)
                .orElseThrow(() -> new UnauthorizedException("Verification token is invalid or already used"));

        if (token.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new UnauthorizedException("Verification token has expired");
        }

        token.setIsUsed(true);
        token.setUsedAt(OffsetDateTime.now());
        emailVerificationTokenRepository.save(token);

        User user = token.getUser();
        user.setIsEmailVerified(true);
        userRepository.save(user);
    }

    @Transactional
    public void forgotPassword(String email) {
        userRepository.findByEmailAndDeletedAtIsNull(email).ifPresent(user -> {
            String rawToken = UUID.randomUUID().toString();
            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .user(user)
                    .tokenHash(hashToken(rawToken))
                    .expiresAt(OffsetDateTime.now().plusHours(1))
                    .isUsed(false)
                    .build();
            passwordResetTokenRepository.save(resetToken);
            emailService.sendPasswordResetEmail(email, rawToken);
        });
        // Always return success to prevent email enumeration
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        String hash = hashToken(req.getToken());
        PasswordResetToken token = passwordResetTokenRepository
                .findByTokenHashAndIsUsedFalse(hash)
                .orElseThrow(() -> new UnauthorizedException("Reset token is invalid or already used"));

        if (token.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new UnauthorizedException("Reset token has expired");
        }

        token.setIsUsed(true);
        token.setUsedAt(OffsetDateTime.now());
        passwordResetTokenRepository.save(token);

        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);

        // Invalidate all sessions after password reset
        refreshTokenRepository.deleteByUserId(user.getId());
        auditLogService.saveAsync(AuditLog.builder()
                .entityType("User").entityId(user.getId())
                .action(AuditAction.PASSWORD_RESET)
                .actorId(user.getId()).actorRole(user.getRole().name()).actorEmail(user.getEmail())
                .build());
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);

        // Invalidate all sessions after password change
        refreshTokenRepository.deleteByUserId(userId);
        auditLogService.saveAsync(AuditLog.builder()
                .entityType("User").entityId(userId)
                .action(AuditAction.PASSWORD_CHANGE)
                .actorId(userId).actorRole(user.getRole().name()).actorEmail(user.getEmail())
                .build());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String createRefreshToken(User user, UUID familyId) {
        String rawToken = UUID.randomUUID().toString();
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(hashToken(rawToken))
                .familyId(familyId)
                .expiresAt(OffsetDateTime.now().plusDays(jwtProperties.getRefreshTokenExpiryDays()))
                .isRevoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    private void revokeToken(RefreshToken token) {
        token.setIsRevoked(true);
        token.setRevokedAt(OffsetDateTime.now());
        refreshTokenRepository.save(token);
    }

    private void saveEmailVerificationToken(User user, String rawToken) {
        EmailVerificationToken token = EmailVerificationToken.builder()
                .user(user)
                .tokenHash(hashToken(rawToken))
                .expiresAt(OffsetDateTime.now().plusDays(1))
                .isUsed(false)
                .build();
        emailVerificationTokenRepository.save(token);
    }

    private AuthResponse buildAuthResponse(User user, String rawRefreshToken) {
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .expiresIn(jwtProperties.getAccessTokenExpiryMs())
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .emailVerified(Boolean.TRUE.equals(user.getIsEmailVerified()))
                .build();
    }

    private void handleFailedLogin(User user) {
        short attempts = (short) (user.getFailedLoginAttempts() + 1);
        user.setFailedLoginAttempts(attempts);
        if (attempts >= 5) {
            user.setLockedUntil(OffsetDateTime.now().plusMinutes(15));
            log.warn("Account locked for user {} after {} failed attempts", user.getEmail(), attempts);
        }
        userRepository.save(user);
    }

    private void resetFailedLoginAttempts(User user) {
        if (user.getFailedLoginAttempts() > 0 || user.getLockedUntil() != null) {
            user.setFailedLoginAttempts((short) 0);
            user.setLockedUntil(null);
            userRepository.save(user);
        }
    }

    private void validateStudentFields(RegisterRequest req) {
        if (req.getInstitution() == null || req.getInstitution().isBlank()) {
            throw new IllegalArgumentException("institution is required for students");
        }
        if (req.getDepartment() == null || req.getDepartment().isBlank()) {
            throw new IllegalArgumentException("department is required for students");
        }
        if (req.getGraduationYear() == null) {
            throw new IllegalArgumentException("graduationYear is required for students");
        }
    }

    public static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
