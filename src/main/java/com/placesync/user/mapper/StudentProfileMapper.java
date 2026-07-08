package com.placesync.user.mapper;

import com.placesync.user.dto.*;
import com.placesync.user.entity.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StudentProfileMapper {

    @Mapping(source = "user.id", target = "userId")
    @Mapping(target = "profilePublic", expression = "java(Boolean.TRUE.equals(profile.getIsProfilePublic()))")
    StudentProfileResponse toResponse(StudentProfile profile);

    StudentSkillResponse toSkillResponse(StudentSkill skill);

    StudentEducationResponse toEducationResponse(StudentEducation education);

    StudentExperienceResponse toExperienceResponse(StudentExperience experience);
}
