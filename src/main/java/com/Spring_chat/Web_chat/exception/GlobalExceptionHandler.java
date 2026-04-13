package com.Spring_chat.Web_chat.exception;

import com.Spring_chat.Web_chat.service.InvalidRefreshTokenException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ── Validation errors (400) ───────────────────────────────────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<FieldErrorDetail> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value",
                        (a, b) -> a
                ))
                .entrySet().stream()
                .map(e -> FieldErrorDetail.of(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
        return ApiErrorBuilder.toResponseEntity(ErrorCode.VALIDATION_FAILED, req.getRequestURI(), fieldErrors);
    }

    // ── Invalid JSON body (400) ───────────────────────────────────────────────
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidJson(
            HttpMessageNotReadableException ex, HttpServletRequest req) {
        return ApiErrorBuilder.toResponseEntity(ErrorCode.INVALID_JSON, req.getRequestURI(), null);
    }

    // ── Missing required request parameter (400) ──────────────────────────────
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingParam(
            MissingServletRequestParameterException ex, HttpServletRequest req) {
        String detail = "Thiếu tham số: " + ex.getParameterName();
        return ApiErrorBuilder.toResponseEntity(ErrorCode.MISSING_PARAMETER, detail, req.getRequestURI(), null);
    }

    // ── Application-level domain errors ──────────────────────────────────────
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiErrorResponse> handleAppException(
            AppException ex, HttpServletRequest req) {
        return ApiErrorBuilder.toResponseEntity(ex.getErrorCode(), ex.getMessage(), req.getRequestURI(), null);
    }

    // ── Refresh token errors (401) ────────────────────────────────────────────
    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidRefreshToken(
            InvalidRefreshTokenException ex, HttpServletRequest req) {
        return ApiErrorBuilder.toResponseEntity(ErrorCode.INVALID_REFRESH_TOKEN, ex.getMessage(), req.getRequestURI(), null);
    }

    // ── Authentication failures (401) ─────────────────────────────────────────
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest req) {
        return ApiErrorBuilder.toResponseEntity(ErrorCode.INVALID_CREDENTIALS, req.getRequestURI(), null);
    }

    // ── Account status (403) ──────────────────────────────────────────────────
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiErrorResponse> handleDisabled(
            DisabledException ex, HttpServletRequest req) {
        return ApiErrorBuilder.toResponseEntity(ErrorCode.ACCOUNT_DISABLED, req.getRequestURI(), null);
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiErrorResponse> handleLocked(
            LockedException ex, HttpServletRequest req) {
        return ApiErrorBuilder.toResponseEntity(ErrorCode.ACCOUNT_BANNED, req.getRequestURI(), null);
    }

    // ── Fallback (500) ────────────────────────────────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(
            Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception on {} {}", req.getMethod(), req.getRequestURI(), ex);
        return ApiErrorBuilder.toResponseEntity(ErrorCode.INTERNAL_ERROR, req.getRequestURI(), null);
    }
}
