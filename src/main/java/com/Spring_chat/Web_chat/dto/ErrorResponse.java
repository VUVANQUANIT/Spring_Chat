package com.Spring_chat.Web_chat.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    @JsonFormat(shape = JsonFormat.Shape.STRING,pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    private int status;
    private String error;
    /** Mã lỗi cho frontend: TOKEN_EXPIRED, REFRESH_TOKEN_EXPIRED, INVALID_TOKEN, INVALID_CREDENTIALS, ... */
    private String code;
    private String message;
    private String path;
    private List<ValidationError> validationErrors;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public  static class ValidationError{
        private String field;
        private String message;
        private Object rejectedValue;
    }
}
