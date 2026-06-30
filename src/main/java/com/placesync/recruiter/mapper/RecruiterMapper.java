package com.placesync.recruiter.mapper;

import com.placesync.recruiter.dto.RecruiterProfileResponse;
import com.placesync.recruiter.entity.RecruiterProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RecruiterMapper {

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "company.id", target = "companyId")
    @Mapping(source = "company.name", target = "companyName")
    RecruiterProfileResponse toResponse(RecruiterProfile profile);
}
