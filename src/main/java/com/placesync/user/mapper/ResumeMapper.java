package com.placesync.user.mapper;

import com.placesync.user.dto.ResumeResponse;
import com.placesync.user.entity.Resume;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ResumeMapper {

    ResumeResponse toResponse(Resume resume);
}
