package com.placesync.auth.service;

import com.placesync.auth.dto.*;
import com.placesync.auth.entity.EmailVerificationToken;
import com.placesync.auth.entity.RefreshToken;
import com.placesync.auth.repository.EmailVerificationTokenRepository;
import com.placesync.auth.repository.PasswordResetTokenRepository;
import com.placesync.auth.repository.RefreshTokenRepository;
import com.placesync.common.audit.service.AuditLogService;
import com.placesync.common.config.JwtProperties;
import com.placesync.common.exception.ConflictException;
import com.placesync.common.exception.UnauthorizedException;
import com.placesync.common.security.JwtTokenProvider;
import com.placesync.recruiter.repository.RecruiterProfileRepository;
import com.placesync.user.entity.User;
import com.placesync.user.entity.UserRole;
import com.placesync.user.repository.StudentProfileRepository;
import com.placesync.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock StudentProfileRepository studentProfileRepository;
    @Mock RecruiterProfileRepository recruiterProfileRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock EmailVerificationTokenRepository emailVerificationTokenRepository;
    @Mock PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock JwtTokenProvider jwtTokenProvider;
    @Mock PasswordEncoder passwordEncoder;
    @Mock EmailService emailService;
    @Mock JwtProperties jwtProperties;
    @Mock AuditLogService auditLogService;

    @InjectMocks AuthService authService;

    private RegisterRequest recruiterRegisterRequest() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("recruiter@test.com");
        req.setPassword("password123");
        req.setRole(UserRole.ROLE_RECRUITER);
        req.setFirstName("John");
        req.setLastName("Doe");
        return req;
    }

    private RegisterRequest studentRegisterRequest() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("student@test.com");
        req.setPassword("password123");
        req.setRole(UserRole.ROLE_STUDENT);
        req.setFirstName("Jane");
        req.setLastName("Smith");
        req.setInstitution("IIT");
        req.setDepartment("CS");
        req.setGraduationYear((short) 2026);
        return req;
    }

    private User activeUser(String email) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setPasswordHash("hashed");
        user.setRole(UserRole.ROLE_STUDENT);
        user.setIsActive(true);
        user.setFailedLoginAttempts((short) 0);
        return user;
    }

    @Test
    void register_newRecruiter_savesUserAndProfile() {
        when(userRepository.existsByEmail("recruiter@test.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        User savedUser = new User();
        savedUser.setId(UUID.randomUUID());
        savedUser.setRole(UserRole.ROLE_RECRUITER);
        when(userRepository.save(any())).thenReturn(savedUser);
        when(jwtProperties.getRefreshTokenExpiryDays()).thenReturn(7);
        when(jwtTokenProvider.generateAccessToken(any(), any(), any())).thenReturn("access-token");

        AuthResponse response = authService.register(recruiterRegisterRequest());

        verify(recruiterProfileRepository).save(any());
        verify(emailVerificationTokenRepository).save(any());
        assertThat(response.getAccessToken()).isEqualTo("access-token");
    }

    @Test
    void register_newStudent_savesUserAndStudentProfile() {
        when(userRepository.existsByEmail("student@test.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        User savedUser = new User();
        savedUser.setId(UUID.randomUUID());
        savedUser.setRole(UserRole.ROLE_STUDENT);
        when(userRepository.save(any())).thenReturn(savedUser);
        when(jwtProperties.getRefreshTokenExpiryDays()).thenReturn(7);
        when(jwtTokenProvider.generateAccessToken(any(), any(), any())).thenReturn("access-token");

        authService.register(studentRegisterRequest());

        verify(studentProfileRepository).save(any());
    }

    @Test
    void register_duplicateEmail_throwsConflictException() {
        when(userRepository.existsByEmail("recruiter@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(recruiterRegisterRequest()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    void register_adminRole_throwsIllegalArgumentException() {
        RegisterRequest req = recruiterRegisterRequest();
        req.setRole(UserRole.ROLE_ADMIN);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void register_studentMissingInstitution_throwsIllegalArgumentException() {
        RegisterRequest req = studentRegisterRequest();
        req.setInstitution(null);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("institution");
    }

    @Test
    void login_validCredentials_returnsAuthResponse() {
        User user = activeUser("student@test.com");
        LoginRequest req = new LoginRequest();
        req.setEmail("student@test.com");
        req.setPassword("password123");
        when(userRepository.findByEmailAndDeletedAtIsNull("student@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtProperties.getRefreshTokenExpiryDays()).thenReturn(7);
        when(jwtTokenProvider.generateAccessToken(any(), any(), any())).thenReturn("access-token");

        AuthResponse response = authService.login(req);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
    }

    @Test
    void login_wrongPassword_throwsUnauthorizedException() {
        User user = activeUser("student@test.com");
        LoginRequest req = new LoginRequest();
        req.setEmail("student@test.com");
        req.setPassword("wrong");
        when(userRepository.findByEmailAndDeletedAtIsNull("student@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid email or password");
        verify(userRepository).save(user);
    }

    @Test
    void login_deactivatedAccount_throwsUnauthorizedException() {
        User user = activeUser("student@test.com");
        user.setIsActive(false);
        LoginRequest req = new LoginRequest();
        req.setEmail("student@test.com");
        req.setPassword("password123");
        when(userRepository.findByEmailAndDeletedAtIsNull("student@test.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("deactivated");
    }

    @Test
    void login_lockedAccount_throwsUnauthorizedException() {
        User user = activeUser("student@test.com");
        user.setLockedUntil(OffsetDateTime.now().plusMinutes(10));
        LoginRequest req = new LoginRequest();
        req.setEmail("student@test.com");
        req.setPassword("password123");
        when(userRepository.findByEmailAndDeletedAtIsNull("student@test.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("locked");
    }

    @Test
    void login_unknownEmail_throwsUnauthorizedException() {
        LoginRequest req = new LoginRequest();
        req.setEmail("unknown@test.com");
        req.setPassword("password123");
        when(userRepository.findByEmailAndDeletedAtIsNull("unknown@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void refresh_validToken_returnsNewAuthResponse() {
        User user = activeUser("student@test.com");
        String rawToken = UUID.randomUUID().toString();
        String hash = AuthService.hashToken(rawToken);
        RefreshToken stored = RefreshToken.builder()
                .user(user)
                .tokenHash(hash)
                .familyId(UUID.randomUUID())
                .expiresAt(OffsetDateTime.now().plusDays(7))
                .isRevoked(false)
                .build();
        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken(rawToken);
        when(refreshTokenRepository.findByTokenHashAndIsRevokedFalse(hash)).thenReturn(Optional.of(stored));
        when(jwtProperties.getRefreshTokenExpiryDays()).thenReturn(7);
        when(jwtTokenProvider.generateAccessToken(any(), any(), any())).thenReturn("new-access-token");

        AuthResponse response = authService.refresh(req);

        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        verify(refreshTokenRepository, atLeast(2)).save(any());
    }

    @Test
    void refresh_expiredToken_throwsUnauthorizedException() {
        User user = activeUser("student@test.com");
        String rawToken = UUID.randomUUID().toString();
        String hash = AuthService.hashToken(rawToken);
        RefreshToken stored = RefreshToken.builder()
                .user(user)
                .tokenHash(hash)
                .familyId(UUID.randomUUID())
                .expiresAt(OffsetDateTime.now().minusDays(1))
                .isRevoked(false)
                .build();
        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken(rawToken);
        when(refreshTokenRepository.findByTokenHashAndIsRevokedFalse(hash)).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> authService.refresh(req))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void verifyEmail_validToken_setsEmailVerified() {
        User user = activeUser("student@test.com");
        String rawToken = UUID.randomUUID().toString();
        String hash = AuthService.hashToken(rawToken);
        EmailVerificationToken token = EmailVerificationToken.builder()
                .user(user)
                .tokenHash(hash)
                .expiresAt(OffsetDateTime.now().plusDays(1))
                .isUsed(false)
                .build();
        when(emailVerificationTokenRepository.findByTokenHashAndIsUsedFalse(hash)).thenReturn(Optional.of(token));

        authService.verifyEmail(rawToken);

        assertThat(token.getIsUsed()).isTrue();
        assertThat(user.getIsEmailVerified()).isTrue();
    }

    @Test
    void hashToken_deterministicOutput() {
        String raw = "test-token";
        String hash1 = AuthService.hashToken(raw);
        String hash2 = AuthService.hashToken(raw);
        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).isNotEqualTo(raw);
    }
}
