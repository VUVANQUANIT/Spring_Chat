package com.Spring_chat.Web_chat.exception;

/**
 * Exception domain-level với ErrorCode tích hợp.
 * Handled globally bởi {@link GlobalExceptionHandler}.
 */
public class AppException extends RuntimeException {

    private final ErrorCode errorCode;

    public AppException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public AppException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
