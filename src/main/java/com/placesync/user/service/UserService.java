package com.placesync.user.service;

import com.placesync.common.exception.ConflictException;
import com.placesync.common.exception.ResourceNotFoundException;
import com.placesync.common.storage.FileValidationService;
import com.placesync.common.storage.S3StorageService;
import com.placesync.user.dto.*;
import com.placesync.user.mapper.StudentProfileMapper;
import com.placesync.user.entity.*;
import com.placesync.user.repository.*;
import static com.placesync.common.util.LogSanitizer.sanitize;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("java:S2629")
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
    private final S3StorageService s3StorageService;
    private final FileValidationService fileValidationService;

    @Value("${app.aws.bucket-pictures}")
    private String picturesBucket;

    @Transactional(readOnly = true)
    public StudentProfileResponse getMyProfile(UUID userId) {
        StudentProfile profile = studentProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(STUDENT_PROFILE, userId));
        StudentProfileResponse response = studentProfileMapper.toResponse(profile);
        if (profile.getProfilePictureS3Key() != null) {
            String pictureUrl = s3StorageService.generatePresignedGetUrl(picturesBucket, profile.getProfilePictureS3Key(), 60);
            response = response.toBuilder().profilePictureUrl(pictureUrl).build();
        }
        return response;
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

        StudentProfile saved = studentProfileRepository.save(profile);
        StudentProfileResponse response = studentProfileMapper.toResponse(saved);
        if (saved.getProfilePictureS3Key() != null) {
            String pictureUrl = s3StorageService.generatePresignedGetUrl(picturesBucket, saved.getProfilePictureS3Key(), 60);
            response = response.toBuilder().profilePictureUrl(pictureUrl).build();
        }
        return response;
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
    public StudentEducationResponse addEducation(UUID userId, StudentEducationRequest req) {
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
        return studentProfileMapper.toEducationResponse(studentEducationRepository.save(education));
    }

    @Transactional
    public StudentEducationResponse updateEducation(UUID userId, UUID educationId, StudentEducationRequest req) {
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
        return studentProfileMapper.toEducationResponse(studentEducationRepository.save(education));
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
    public StudentExperienceResponse addExperience(UUID userId, StudentExperienceRequest req) {
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
        return studentProfileMapper.toExperienceResponse(studentExperienceRepository.save(experience));
    }

    @Transactional
    public StudentExperienceResponse updateExperience(UUID userId, UUID experienceId, StudentExperienceRequest req) {
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
        return studentProfileMapper.toExperienceResponse(studentExperienceRepository.save(experience));
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
    public List<StudentSkillResponse> getSkills(UUID userId) {
        StudentProfile profile = requireStudentProfile(userId);
        return studentSkillRepository.findByStudentId(profile.getId())
                .stream()
                .map(studentProfileMapper::toSkillResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StudentEducationResponse> getEducation(UUID userId) {
        StudentProfile profile = requireStudentProfile(userId);
        return studentEducationRepository.findByStudentIdOrderByStartYearDesc(profile.getId())
                .stream()
                .map(studentProfileMapper::toEducationResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StudentExperienceResponse> getExperience(UUID userId) {
        StudentProfile profile = requireStudentProfile(userId);
        return studentExperienceRepository.findByStudentIdOrderByStartDateDesc(profile.getId())
                .stream()
                .map(studentProfileMapper::toExperienceResponse)
                .toList();
    }

    @Transactional
    public StudentProfileResponse uploadProfilePicture(UUID userId, MultipartFile file) {
        log.info("Uploading profile picture for userId={}", sanitize(userId));
        fileValidationService.validateProfileImage(file);

        StudentProfile profile = requireStudentProfile(userId);
        String s3Key = "profile-pictures/" + userId + "/" + Instant.now().toEpochMilli() + "-" + UUID.randomUUID() + ".jpg";

        try {
            s3StorageService.uploadFile(picturesBucket, s3Key,
                    file.getInputStream(), file.getContentType(), file.getSize());
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read uploaded file");
        }

        profile.setProfilePictureS3Key(s3Key);
        studentProfileRepository.save(profile);

        String pictureUrl = s3StorageService.generatePresignedGetUrl(picturesBucket, s3Key, 60);
        return studentProfileMapper.toResponse(profile).toBuilder()
                .profilePictureUrl(pictureUrl)
                .build();
    }

    private StudentProfile requireStudentProfile(UUID userId) {
        return studentProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(STUDENT_PROFILE, userId));
    }
}
