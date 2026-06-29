package com.placesync.common.util;

import com.placesync.common.logging.MdcLoggingFilter;
import org.slf4j.MDC;

import java.time.OffsetDateTime;

public class ApiResponseFactory {

    private ApiResponseFactory() {}

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, OffsetDateTime.now(), MDC.get(MdcLoggingFilter.CORRELATION_ID_MDC_KEY));
    }
}
