package com.Spring_chat.Web_chat.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Cấu trúc response lỗi chuẩn cho toàn bộ API.
 * FE có thể switch trên field {@code code} để xử lý từng loại lỗi.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
        String timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        String traceId,
        List<FieldErrorDetail> errors
) {}
