package com.placesync.user.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class StudentSkillResponse {
    private UUID id;
    private String skillName;
}
