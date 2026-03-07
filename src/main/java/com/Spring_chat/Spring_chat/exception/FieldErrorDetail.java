package com.Spring_chat.Spring_chat.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FieldErrorDetail(
        String field,
        String message,
        Object rejectedValue
) {
    public static FieldErrorDetail of(String field, String message) {
        return new FieldErrorDetail(field, message, null);
    }

    public static FieldErrorDetail of(String field, String message, Object rejectedValue) {
        return new FieldErrorDetail(field, message, rejectedValue);
    }
}
