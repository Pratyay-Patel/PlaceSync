package com.placesync.user.service;

import com.placesync.common.exception.ResourceNotFoundException;
import com.placesync.user.dto.CreateResumeRequest;
import com.placesync.user.dto.ResumeResponse;
import com.placesync.user.entity.Resume;
import com.placesync.user.entity.StudentProfile;
import com.placesync.user.mapper.ResumeMapper;
import com.placesync.user.repository.ResumeRepository;
import com.placesync.user.repository.StudentProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
        return r;
    }

    @Test
    void createResume_nonDefault_savesWithoutClearingExisting() {
        StudentProfile profile = profile();
        CreateResumeRequest req = new CreateResumeRequest();
        req.setLabel("CV");
        req.setOriginalFilename("cv.pdf");
        req.setIsDefault(false);
        when(studentProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(resumeRepository.save(any())).thenReturn(new Resume());
        when(resumeMapper.toResponse(any())).thenReturn(new ResumeResponse());

        resumeService.createResume(userId, req);

        verify(resumeRepository, never()).findByStudentIdAndIsDefaultTrueAndDeletedAtIsNull(any());
        verify(resumeRepository).save(any(Resume.class));
    }

    @Test
    void createResume_isDefault_clearsExistingDefault() {
        StudentProfile profile = profile();
        Resume existingDefault = resume(profile);
        existingDefault.setIsDefault(true);
        CreateResumeRequest req = new CreateResumeRequest();
        req.setLabel("CV");
        req.setOriginalFilename("cv.pdf");
        req.setIsDefault(true);
        when(studentProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(resumeRepository.findByStudentIdAndIsDefaultTrueAndDeletedAtIsNull(profile.getId()))
                .thenReturn(Optional.of(existingDefault));
        when(resumeRepository.save(any())).thenReturn(new Resume());
        when(resumeMapper.toResponse(any())).thenReturn(new ResumeResponse());

        resumeService.createResume(userId, req);

        assertThat(existingDefault.getIsDefault()).isFalse();
        verify(resumeRepository, atLeast(2)).save(any());
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

        org.assertj.core.api.Assertions.assertThat(r.getDeletedAt()).isNotNull();
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
