package com.placesync.application.mapper;

import com.placesync.application.dto.ApplicationResponse;
import com.placesync.application.entity.Application;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ApplicationMapper {

    @Mapping(source = "student.id", target = "studentId")
    @Mapping(source = "student.firstName", target = "studentFirstName")
    @Mapping(source = "student.lastName", target = "studentLastName")
    @Mapping(source = "job.id", target = "jobId")
    @Mapping(source = "job.title", target = "jobTitle")
    @Mapping(source = "job.company.name", target = "companyName")
    @Mapping(source = "resume.id", target = "resumeId")
    @Mapping(source = "resume.label", target = "resumeLabel")
    ApplicationResponse toResponse(Application application);
}
