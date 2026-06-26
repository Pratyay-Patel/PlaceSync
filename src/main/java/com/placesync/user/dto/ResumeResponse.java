package com.placesync.user.dto;

import com.placesync.user.entity.Resume;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeResponse {

    private UUID id;
    private String label;
    private String originalFilename;
    private Long fileSizeBytes;
    private Boolean isDefault;
    private OffsetDateTime uploadedAt;

    public static ResumeResponse from(Resume resume) {
        return ResumeResponse.builder()
                .id(resume.getId())
                .label(resume.getLabel())
                .originalFilename(resume.getOriginalFilename())
                .fileSizeBytes(resume.getFileSizeBytes())
                .isDefault(resume.getIsDefault())
                .uploadedAt(resume.getUploadedAt())
                .build();
    }
}
