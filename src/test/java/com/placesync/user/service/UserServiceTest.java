package com.placesync.user.service;

import com.placesync.common.exception.ConflictException;
import com.placesync.common.exception.ResourceNotFoundException;
import com.placesync.user.dto.*;
import com.placesync.user.entity.*;
import com.placesync.user.mapper.StudentProfileMapper;
import com.placesync.user.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock StudentProfileRepository studentProfileRepository;
    @Mock StudentProfileMapper studentProfileMapper;
    @Mock StudentSkillRepository studentSkillRepository;
    @Mock StudentEducationRepository studentEducationRepository;
    @Mock StudentExperienceRepository studentExperienceRepository;

    @InjectMocks UserService userService;

    private final UUID userId = UUID.randomUUID();

    private StudentProfile studentProfile() {
        StudentProfile p = new StudentProfile();
        p.setId(UUID.randomUUID());
        return p;
    }

    @Test
    void getMyProfile_found_returnsMappedResponse() {
        StudentProfile profile = studentProfile();
        StudentProfileResponse response = StudentProfileResponse.builder().build();
        when(studentProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(studentProfileMapper.toResponse(profile)).thenReturn(response);

        StudentProfileResponse result = userService.getMyProfile(userId);

        assertThat(result).isEqualTo(response);
    }

    @Test
    void getMyProfile_notFound_throwsResourceNotFoundException() {
        when(studentProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getMyProfile(userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateProfile_updatesFieldsAndSaves() {
        StudentProfile profile = studentProfile();
        UpdateStudentProfileRequest req = new UpdateStudentProfileRequest();
        req.setFirstName("Jane");
        req.setLastName("Doe");
        req.setIsProfilePublic(true);
        when(studentProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(studentProfileRepository.save(profile)).thenReturn(profile);
        when(studentProfileMapper.toResponse(profile)).thenReturn(StudentProfileResponse.builder().build());

        userService.updateProfile(userId, req);

        assertThat(profile.getFirstName()).isEqualTo("Jane");
        verify(studentProfileRepository).save(profile);
    }

    @Test
    void addSkill_newSkill_savesSuccessfully() {
        StudentProfile profile = studentProfile();
        StudentSkillRequest req = new StudentSkillRequest();
        req.setSkillName("Java");
        when(studentProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(studentSkillRepository.existsByStudentIdAndSkillName(profile.getId(), "Java")).thenReturn(false);

        userService.addSkill(userId, req);

        verify(studentSkillRepository).save(any(StudentSkill.class));
    }

    @Test
    void addSkill_duplicateSkill_throwsConflictException() {
        StudentProfile profile = studentProfile();
        StudentSkillRequest req = new StudentSkillRequest();
        req.setSkillName("Java");
        when(studentProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(studentSkillRepository.existsByStudentIdAndSkillName(profile.getId(), "Java")).thenReturn(true);

        assertThatThrownBy(() -> userService.addSkill(userId, req))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void removeSkill_ownSkill_deletesSuccessfully() {
        StudentProfile profile = studentProfile();
        UUID skillId = UUID.randomUUID();
        StudentSkill skill = new StudentSkill();
        skill.setStudent(profile);
        when(studentProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(studentSkillRepository.findById(skillId)).thenReturn(Optional.of(skill));

        userService.removeSkill(userId, skillId);

        verify(studentSkillRepository).delete(skill);
    }

    @Test
    void removeSkill_skillBelongsToOtherStudent_throwsAccessDeniedException() {
        StudentProfile profile = studentProfile();
        StudentProfile otherProfile = studentProfile();
        UUID skillId = UUID.randomUUID();
        StudentSkill skill = new StudentSkill();
        skill.setStudent(otherProfile);
        when(studentProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(studentSkillRepository.findById(skillId)).thenReturn(Optional.of(skill));

        assertThatThrownBy(() -> userService.removeSkill(userId, skillId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void addEducation_savesAndReturnsEntity() {
        StudentProfile profile = studentProfile();
        StudentEducationRequest req = new StudentEducationRequest();
        req.setDegree("B.Tech");
        req.setInstitution("IIT");
        req.setStartYear((short) 2020);
        StudentEducation saved = new StudentEducation();
        when(studentProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(studentEducationRepository.save(any())).thenReturn(saved);

        StudentEducation result = userService.addEducation(userId, req);

        assertThat(result).isEqualTo(saved);
    }

    @Test
    void deleteEducation_belongsToOtherStudent_throwsAccessDeniedException() {
        StudentProfile profile = studentProfile();
        StudentProfile other = studentProfile();
        UUID eduId = UUID.randomUUID();
        StudentEducation edu = new StudentEducation();
        edu.setStudent(other);
        when(studentProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(studentEducationRepository.findById(eduId)).thenReturn(Optional.of(edu));

        assertThatThrownBy(() -> userService.deleteEducation(userId, eduId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void addExperience_savesAndReturnsEntity() {
        StudentProfile profile = studentProfile();
        StudentExperienceRequest req = new StudentExperienceRequest();
        req.setCompanyName("Acme");
        req.setRole("Intern");
        StudentExperience saved = new StudentExperience();
        when(studentProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(studentExperienceRepository.save(any())).thenReturn(saved);

        StudentExperience result = userService.addExperience(userId, req);

        assertThat(result).isEqualTo(saved);
    }

    @Test
    void deleteExperience_belongsToOtherStudent_throwsAccessDeniedException() {
        StudentProfile profile = studentProfile();
        StudentProfile other = studentProfile();
        UUID expId = UUID.randomUUID();
        StudentExperience exp = new StudentExperience();
        exp.setStudent(other);
        when(studentProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(studentExperienceRepository.findById(expId)).thenReturn(Optional.of(exp));

        assertThatThrownBy(() -> userService.deleteExperience(userId, expId))
                .isInstanceOf(AccessDeniedException.class);
    }
}
