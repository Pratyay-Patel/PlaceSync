package com.placesync.common.util;

import java.time.OffsetDateTime;

public record ApiResponse<T>(
        boolean success,
        T data,
        OffsetDateTime timestamp,
        String correlationId
) {}
