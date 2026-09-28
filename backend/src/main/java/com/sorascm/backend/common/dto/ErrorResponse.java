package com.sorascm.backend.common.dto;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        int status,
        String errorCode,
        String message,
        String path,
        Instant timestamp,
        List<ValidationError> errors
) {
    public record ValidationError(String field, String message) {}

    public static ErrorResponse of(int status, String errorCode, String message, String path) {
        return new ErrorResponse(status, errorCode, message, path, Instant.now(), List.of());
    }

    public static ErrorResponse of(int status, String errorCode, String message, String path, List<ValidationError> errors) {
        return new ErrorResponse(status, errorCode, message, path, Instant.now(), errors);
    }
}