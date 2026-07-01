package com.placesync.user.service;

import com.placesync.common.exception.ConflictException;
import com.placesync.common.exception.ResourceNotFoundException;
import com.placesync.user.dto.*;
import com.placesync.user.mapper.StudentProfileMapper;
import com.placesync.user.entity.*;
import com.placesync.user.repository.*;
import static com.placesync.common.util.LogSanitizer.sanitize;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private static final String STUDENT_PROFILE = "StudentProfile";

    private final StudentProfileRepository studentProfileRepository;
    private final StudentProfileMapper studentProfileMapper;
    private final StudentSkillRepository studentSkillRepository;
    private final StudentEducationRepository studentEducationRepository;
    private final StudentExperienceRepository studentExperienceRepository;

    @Transactional(readOnly = true)
    public StudentProfileResponse getMyProfile(UUID userId) {
        StudentProfile profile = studentProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(STUDENT_PROFILE, userId));
        return studentProfileMapper.toResponse(profile);
    }

    @Transactional
    public StudentProfileResponse updateProfile(UUID userId, UpdateStudentProfileRequest req) {
        log.info("Updating student profile for userId={}", sanitize(userId));
        StudentProfile profile = studentProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(STUDENT_PROFILE, userId));

        profile.setFirstName(req.getFirstName());
        profile.setLastName(req.getLastName());
        profile.setPhone(req.getPhone());
        profile.setDateOfBirth(req.getDateOfBirth());
        profile.setGender(req.getGender());
        profile.setInstitution(req.getInstitution());
        profile.setDepartment(req.getDepartment());
        profile.setGraduationYear(req.getGraduationYear());
        profile.setCgpa(req.getCgpa());
        profile.setBio(req.getBio());
        if (req.getIsProfilePublic() != null) {
            profile.setIsProfilePublic(req.getIsProfilePublic());
        }

        return studentProfileMapper.toResponse(studentProfileRepository.save(profile));
    }

    @Transactional
    public void addSkill(UUID userId, StudentSkillRequest req) {
        log.info("Adding skill for userId={}", sanitize(userId));
        StudentProfile profile = requireStudentProfile(userId);
        if (studentSkillRepository.existsByStudentIdAndSkillName(profile.getId(), req.getSkillName())) {
            throw new ConflictException("Skill already added: " + req.getSkillName());
        }
        StudentSkill skill = StudentSkill.builder()
                .student(profile)
                .skillName(req.getSkillName())
                .build();
        studentSkillRepository.save(skill);
    }

    @Transactional
    public void removeSkill(UUID userId, UUID skillId) {
        log.info("Removing skillId={} for userId={}", sanitize(skillId), sanitize(userId));
        StudentProfile profile = requireStudentProfile(userId);
        StudentSkill skill = studentSkillRepository.findById(skillId)
                .orElseThrow(() -> new ResourceNotFoundException("Skill", skillId));
        if (!skill.getStudent().getId().equals(profile.getId())) {
            throw new AccessDeniedException("Skill does not belong to this student");
        }
        studentSkillRepository.delete(skill);
    }

    @Transactional
    public StudentEducation addEducation(UUID userId, StudentEducationRequest req) {
        log.info("Adding education record for userId={}", sanitize(userId));
        StudentProfile profile = requireStudentProfile(userId);
        StudentEducation education = StudentEducation.builder()
                .student(profile)
                .degree(req.getDegree())
                .institution(req.getInstitution())
                .fieldOfStudy(req.getFieldOfStudy())
                .startYear(req.getStartYear())
                .endYear(req.getEndYear())
                .percentageOrCgpa(req.getPercentageOrCgpa())
                .build();
        return studentEducationRepository.save(education);
    }

    @Transactional
    public StudentEducation updateEducation(UUID userId, UUID educationId, StudentEducationRequest req) {
        log.info("Updating educationId={} for userId={}", sanitize(educationId), sanitize(userId));
        StudentProfile profile = requireStudentProfile(userId);
        StudentEducation education = studentEducationRepository.findById(educationId)
                .orElseThrow(() -> new ResourceNotFoundException("Education", educationId));
        if (!education.getStudent().getId().equals(profile.getId())) {
            throw new AccessDeniedException("Education record does not belong to this student");
        }
        education.setDegree(req.getDegree());
        education.setInstitution(req.getInstitution());
        education.setFieldOfStudy(req.getFieldOfStudy());
        education.setStartYear(req.getStartYear());
        education.setEndYear(req.getEndYear());
        education.setPercentageOrCgpa(req.getPercentageOrCgpa());
        return studentEducationRepository.save(education);
    }

    @Transactional
    public void deleteEducation(UUID userId, UUID educationId) {
        log.info("Deleting educationId={} for userId={}", sanitize(educationId), sanitize(userId));
        StudentProfile profile = requireStudentProfile(userId);
        StudentEducation education = studentEducationRepository.findById(educationId)
                .orElseThrow(() -> new ResourceNotFoundException("Education", educationId));
        if (!education.getStudent().getId().equals(profile.getId())) {
            throw new AccessDeniedException("Education record does not belong to this student");
        }
        studentEducationRepository.delete(education);
    }

    @Transactional
    public StudentExperience addExperience(UUID userId, StudentExperienceRequest req) {
        log.info("Adding experience record for userId={}", sanitize(userId));
        StudentProfile profile = requireStudentProfile(userId);
        StudentExperience experience = StudentExperience.builder()
                .student(profile)
                .companyName(req.getCompanyName())
                .role(req.getRole())
                .description(req.getDescription())
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .isCurrent(Boolean.TRUE.equals(req.getIsCurrent()))
                .build();
        return studentExperienceRepository.save(experience);
    }

    @Transactional
    public StudentExperience updateExperience(UUID userId, UUID experienceId, StudentExperienceRequest req) {
        log.info("Updating experienceId={} for userId={}", sanitize(experienceId), sanitize(userId));
        StudentProfile profile = requireStudentProfile(userId);
        StudentExperience experience = studentExperienceRepository.findById(experienceId)
                .orElseThrow(() -> new ResourceNotFoundException("Experience", experienceId));
        if (!experience.getStudent().getId().equals(profile.getId())) {
            throw new AccessDeniedException("Experience record does not belong to this student");
        }
        experience.setCompanyName(req.getCompanyName());
        experience.setRole(req.getRole());
        experience.setDescription(req.getDescription());
        experience.setStartDate(req.getStartDate());
        experience.setEndDate(req.getEndDate());
        experience.setIsCurrent(Boolean.TRUE.equals(req.getIsCurrent()));
        return studentExperienceRepository.save(experience);
    }

    @Transactional
    public void deleteExperience(UUID userId, UUID experienceId) {
        log.info("Deleting experienceId={} for userId={}", sanitize(experienceId), sanitize(userId));
        StudentProfile profile = requireStudentProfile(userId);
        StudentExperience experience = studentExperienceRepository.findById(experienceId)
                .orElseThrow(() -> new ResourceNotFoundException("Experience", experienceId));
        if (!experience.getStudent().getId().equals(profile.getId())) {
            throw new AccessDeniedException("Experience record does not belong to this student");
        }
        studentExperienceRepository.delete(experience);
    }

    @Transactional(readOnly = true)
    public List<StudentSkill> getSkills(UUID userId) {
        StudentProfile profile = requireStudentProfile(userId);
        return studentSkillRepository.findByStudentId(profile.getId());
    }

    @Transactional(readOnly = true)
    public List<StudentEducation> getEducation(UUID userId) {
        StudentProfile profile = requireStudentProfile(userId);
        return studentEducationRepository.findByStudentIdOrderByStartYearDesc(profile.getId());
    }

    @Transactional(readOnly = true)
    public List<StudentExperience> getExperience(UUID userId) {
        StudentProfile profile = requireStudentProfile(userId);
        return studentExperienceRepository.findByStudentIdOrderByStartDateDesc(profile.getId());
    }

    private StudentProfile requireStudentProfile(UUID userId) {
        return studentProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(STUDENT_PROFILE, userId));
    }
}
