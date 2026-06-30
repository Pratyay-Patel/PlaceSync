package com.placesync.interview.mapper;

import com.placesync.interview.dto.InterviewResponse;
import com.placesync.interview.entity.Interview;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InterviewMapper {

    @Mapping(source = "application.id", target = "applicationId")
    @Mapping(source = "application.student.id", target = "studentId")
    @Mapping(source = "application.student.firstName", target = "studentFirstName")
    @Mapping(source = "application.student.lastName", target = "studentLastName")
    @Mapping(source = "application.job.id", target = "jobId")
    @Mapping(source = "application.job.title", target = "jobTitle")
    @Mapping(source = "application.job.company.name", target = "companyName")
    InterviewResponse toResponse(Interview interview);
}
