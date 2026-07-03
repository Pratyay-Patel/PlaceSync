package com.placesync.user.service;

import com.placesync.application.repository.ApplicationRepository;
import com.placesync.common.exception.ResourceNotFoundException;
import com.placesync.common.storage.FileValidationService;
import com.placesync.common.storage.S3StorageService;
import com.placesync.recruiter.entity.RecruiterProfile;
import com.placesync.recruiter.repository.RecruiterProfileRepository;
import com.placesync.user.dto.ResumeDownloadUrlResponse;
import com.placesync.user.dto.ResumeResponse;
import com.placesync.user.entity.Resume;
import com.placesync.user.entity.StudentProfile;
import com.placesync.user.entity.UserRole;
import com.placesync.user.mapper.ResumeMapper;
import com.placesync.user.repository.ResumeRepository;
import com.placesync.user.repository.StudentProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumeServiceTest {

    @Mock ResumeRepository resumeRepository;
    @Mock ResumeMapper resumeMapper;
    @Mock StudentProfileRepository studentProfileRepository;
    @Mock RecruiterProfileRepository recruiterProfileRepository;
    @Mock ApplicationRepository applicationRepository;
    @Mock S3StorageService s3StorageService;
    @Mock FileValidationService fileValidationService;

    @InjectMocks ResumeService resumeService;

    private final UUID userId = UUID.randomUUID();
    private final UUID resumeId = UUID.randomUUID();

    private StudentProfile profile() {
        StudentProfile p = new StudentProfile();
        p.setId(UUID.randomUUID());
        return p;
    }

    private Resume resume(StudentProfile owner) {
        Resume r = new Resume();
        r.setId(resumeId);
        r.setStudent(owner);
        r.setIsDefault(false);
        r.setS3Key("resumes/key/file.pdf");
        return r;
    }

    private MockMultipartFile pdfFile() {
        byte[] pdfHeader = {0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34};
        return new MockMultipartFile("file", "cv.pdf", "application/pdf", pdfHeader);
    }

    @Test
    void uploadResume_nonDefault_savesWithoutClearingExisting() {
        StudentProfile profile = profile();
        when(studentProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(resumeRepository.save(any())).thenReturn(new Resume());
        when(resumeMapper.toResponse(any())).thenReturn(new ResumeResponse());

        resumeService.uploadResume(userId, pdfFile(), "CV", false);

        verify(fileValidationService).validatePdf(any());
        verify(resumeRepository, never()).findByStudentIdAndIsDefaultTrueAndDeletedAtIsNull(any());
        verify(s3StorageService).uploadFile(any(), any(), any(), any(), anyLong());
        verify(resumeRepository).save(any(Resume.class));
    }

    @Test
    void uploadResume_isDefault_clearsExistingDefault() {
        StudentProfile profile = profile();
        Resume existingDefault = resume(profile);
        existingDefault.setIsDefault(true);
        when(studentProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(resumeRepository.findByStudentIdAndIsDefaultTrueAndDeletedAtIsNull(profile.getId()))
                .thenReturn(Optional.of(existingDefault));
        when(resumeRepository.save(any())).thenReturn(new Resume());
        when(resumeMapper.toResponse(any())).thenReturn(new ResumeResponse());

        resumeService.uploadResume(userId, pdfFile(), "CV", true);

        assertThat(existingDefault.getIsDefault()).isFalse();
        verify(resumeRepository, atLeast(2)).save(any());
    }

    @Test
    void uploadResume_studentNotFound_throwsResourceNotFoundException() {
        when(studentProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resumeService.uploadResume(userId, pdfFile(), "CV", false))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getDownloadUrl_studentOwnsResume_returnsUrl() {
        StudentProfile profile = profile();
        Resume r = resume(profile);
        when(resumeRepository.findById(resumeId)).thenReturn(Optional.of(r));
        when(studentProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(s3StorageService.generatePresignedGetUrl(any(), any(), anyInt())).thenReturn("https://s3.example.com/url");

        ResumeDownloadUrlResponse response = resumeService.getDownloadUrl(userId, resumeId, UserRole.ROLE_STUDENT);

        assertThat(response.downloadUrl()).isEqualTo("https://s3.example.com/url");
        assertThat(response.expiresAt()).isAfter(OffsetDateTime.now());
    }

    @Test
    void getDownloadUrl_studentDoesNotOwnResume_throwsAccessDeniedException() {
        StudentProfile owner = profile();
        StudentProfile caller = profile();
        Resume r = resume(owner);
        when(resumeRepository.findById(resumeId)).thenReturn(Optional.of(r));
        when(studentProfileRepository.findByUserId(userId)).thenReturn(Optional.of(caller));

        assertThatThrownBy(() -> resumeService.getDownloadUrl(userId, resumeId, UserRole.ROLE_STUDENT))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getDownloadUrl_recruiterWithApplication_returnsUrl() {
        StudentProfile owner = profile();
        Resume r = resume(owner);
        RecruiterProfile recruiter = RecruiterProfile.builder().id(UUID.randomUUID()).build();
        when(resumeRepository.findById(resumeId)).thenReturn(Optional.of(r));
        when(recruiterProfileRepository.findByUserId(userId)).thenReturn(Optional.of(recruiter));
        when(applicationRepository.countByStudentAndRecruiter(owner.getId(), recruiter.getId())).thenReturn(1L);
        when(s3StorageService.generatePresignedGetUrl(any(), any(), anyInt())).thenReturn("https://s3.example.com/url");

        ResumeDownloadUrlResponse response = resumeService.getDownloadUrl(userId, resumeId, UserRole.ROLE_RECRUITER);

        assertThat(response.downloadUrl()).isNotBlank();
    }

    @Test
    void getDownloadUrl_recruiterWithNoApplication_throwsAccessDeniedException() {
        StudentProfile owner = profile();
        Resume r = resume(owner);
        RecruiterProfile recruiter = RecruiterProfile.builder().id(UUID.randomUUID()).build();
        when(resumeRepository.findById(resumeId)).thenReturn(Optional.of(r));
        when(recruiterProfileRepository.findByUserId(userId)).thenReturn(Optional.of(recruiter));
        when(applicationRepository.countByStudentAndRecruiter(owner.getId(), recruiter.getId())).thenReturn(0L);

        assertThatThrownBy(() -> resumeService.getDownloadUrl(userId, resumeId, UserRole.ROLE_RECRUITER))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void setDefault_ownedResume_setsDefaultFlag() {
        StudentProfile profile = profile();
        Resume r = resume(profile);
        when(studentProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(resumeRepository.findById(resumeId)).thenReturn(Optional.of(r));
        when(resumeRepository.findByStudentIdAndIsDefaultTrueAndDeletedAtIsNull(profile.getId()))
                .thenReturn(Optional.empty());
        when(resumeRepository.save(r)).thenReturn(r);
        when(resumeMapper.toResponse(r)).thenReturn(new ResumeResponse());

        resumeService.setDefault(userId, resumeId);

        assertThat(r.getIsDefault()).isTrue();
    }

    @Test
    void setDefault_resumeBelongsToOther_throwsAccessDeniedException() {
        StudentProfile profile = profile();
        StudentProfile other = profile();
        Resume r = resume(other);
        when(studentProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(resumeRepository.findById(resumeId)).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> resumeService.setDefault(userId, resumeId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void setDefault_softDeletedResume_throwsResourceNotFoundException() {
        StudentProfile profile = profile();
        Resume r = resume(profile);
        r.setDeletedAt(OffsetDateTime.now());
        when(studentProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(resumeRepository.findById(resumeId)).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> resumeService.setDefault(userId, resumeId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void softDelete_ownedResume_setsDeletedAt() {
        StudentProfile profile = profile();
        Resume r = resume(profile);
        when(studentProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(resumeRepository.findById(resumeId)).thenReturn(Optional.of(r));

        resumeService.softDelete(userId, resumeId);

        assertThat(r.getDeletedAt()).isNotNull();
        verify(resumeRepository).save(r);
    }

    @Test
    void softDelete_alreadyDeleted_throwsResourceNotFoundException() {
        StudentProfile profile = profile();
        Resume r = resume(profile);
        r.setDeletedAt(OffsetDateTime.now());
        when(studentProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(resumeRepository.findById(resumeId)).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> resumeService.softDelete(userId, resumeId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void softDelete_resumeBelongsToOther_throwsAccessDeniedException() {
        StudentProfile profile = profile();
        StudentProfile other = profile();
        Resume r = resume(other);
        when(studentProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(resumeRepository.findById(resumeId)).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> resumeService.softDelete(userId, resumeId))
                .isInstanceOf(AccessDeniedException.class);
    }
}
