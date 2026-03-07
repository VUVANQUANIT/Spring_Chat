package com.Spring_chat.Spring_chat.exception;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;

/**
 * Factory dùng chung để build {@link ApiErrorResponse} nhất quán
 * cho cả GlobalExceptionHandler, Http401EntryPoint và Http403AccessDeniedHandler.
 */
public final class ApiErrorBuilder {

    private ApiErrorBuilder() {}

    public static ApiErrorResponse build(
            ErrorCode errorCode,
            String path,
            List<FieldErrorDetail> fieldErrors
    ) {
        return build(errorCode, errorCode.getDefaultMessage(), path, fieldErrors);
    }

    public static ApiErrorResponse build(
            ErrorCode errorCode,
            String message,
            String path,
            List<FieldErrorDetail> fieldErrors
    ) {
        HttpStatus status = errorCode.getHttpStatus();
        return new ApiErrorResponse(
                Instant.now().toString(),
                status.value(),
                status.getReasonPhrase(),
                errorCode.code(),
                message,
                path,
                MDC.get("traceId"),
                fieldErrors
        );
    }

    public static ResponseEntity<ApiErrorResponse> toResponseEntity(
            ErrorCode errorCode,
            String path,
            List<FieldErrorDetail> fieldErrors
    ) {
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(build(errorCode, path, fieldErrors));
    }

    public static ResponseEntity<ApiErrorResponse> toResponseEntity(
            ErrorCode errorCode,
            String message,
            String path,
            List<FieldErrorDetail> fieldErrors
    ) {
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(build(errorCode, message, path, fieldErrors));
    }
}
