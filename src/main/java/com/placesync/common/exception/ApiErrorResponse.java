package com.placesync.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {

    private final OffsetDateTime timestamp;
    private final int status;
    private final String error;
    private final String message;
    private final String path;
    private final List<FieldError> fieldErrors;

    public ApiErrorResponse(OffsetDateTime timestamp, int status, String error,
                            String message, String path, List<FieldError> fieldErrors) {
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
        this.fieldErrors = fieldErrors;
    }

    public OffsetDateTime getTimestamp()         { return timestamp; }
    public int getStatus()                       { return status; }
    public String getError()                     { return error; }
    public String getMessage()                   { return message; }
    public String getPath()                      { return path; }
    public List<FieldError> getFieldErrors()     { return fieldErrors; }

    public static class FieldError {
        private final String field;
        private final String message;

        public FieldError(String field, String message) {
            this.field = field;
            this.message = message;
        }

        public String getField()   { return field; }
        public String getMessage() { return message; }
    }
}
