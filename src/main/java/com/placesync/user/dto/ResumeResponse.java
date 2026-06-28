package com.placesync.user.dto;

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

}
