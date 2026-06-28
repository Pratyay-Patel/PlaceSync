package com.placesync.job.mapper;

import com.placesync.job.dto.JobResponse;
import com.placesync.job.dto.JobSummaryResponse;
import com.placesync.job.entity.Job;
import com.placesync.job.entity.JobEligibleDepartment;
import com.placesync.job.entity.JobRequiredSkill;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface JobMapper {

    @Mapping(source = "company.id", target = "companyId")
    @Mapping(source = "company.name", target = "companyName")
    @Mapping(source = "recruiter.id", target = "recruiterId")
    @Mapping(source = "recruiter.firstName", target = "recruiterFirstName")
    @Mapping(source = "recruiter.lastName", target = "recruiterLastName")
    @Mapping(source = "requiredSkills", target = "requiredSkills", qualifiedByName = "skillNames")
    @Mapping(source = "eligibleDepartments", target = "eligibleDepartments", qualifiedByName = "departmentNames")
    JobResponse toResponse(Job job);

    @Mapping(source = "company.id", target = "companyId")
    @Mapping(source = "company.name", target = "companyName")
    JobSummaryResponse toSummaryResponse(Job job);

    @Named("skillNames")
    default List<String> skillNames(List<JobRequiredSkill> skills) {
        return skills.stream().map(JobRequiredSkill::getSkillName).collect(Collectors.toList());
    }

    @Named("departmentNames")
    default List<String> departmentNames(List<JobEligibleDepartment> departments) {
        return departments.stream().map(JobEligibleDepartment::getDepartmentName).collect(Collectors.toList());
    }
}
