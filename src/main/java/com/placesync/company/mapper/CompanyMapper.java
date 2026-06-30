package com.placesync.company.mapper;

import com.placesync.company.dto.CompanyResponse;
import com.placesync.company.entity.Company;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CompanyMapper {

    @Mapping(source = "createdBy.id", target = "createdById")
    CompanyResponse toResponse(Company company);
}
