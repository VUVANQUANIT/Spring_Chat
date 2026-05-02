package com.Spring_chat.Web_chat.enums;

import com.Spring_chat.Web_chat.exception.AppException;
import com.Spring_chat.Web_chat.exception.ErrorCode;

import java.util.Locale;

public enum MessageDeleteScope {
    ME,
    ALL;

    public static MessageDeleteScope fromQueryParam(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new AppException(ErrorCode.MISSING_PARAMETER, "Thiếu tham số scope");
        }
        try {
            return MessageDeleteScope.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "scope phải là ME hoặc ALL");
        }
    }
}
