package com.placesync.user.dto;

import java.time.OffsetDateTime;

public record ResumeDownloadUrlResponse(String downloadUrl, OffsetDateTime expiresAt) {}
