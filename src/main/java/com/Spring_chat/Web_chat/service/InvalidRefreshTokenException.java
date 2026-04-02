package com.Spring_chat.Web_chat.service;

import com.Spring_chat.Web_chat.exception.ErrorCode;

public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException(String message) {
        super(message);
    }

    public ErrorCode getErrorCode() {
        return ErrorCode.INVALID_REFRESH_TOKEN;
    }
}
