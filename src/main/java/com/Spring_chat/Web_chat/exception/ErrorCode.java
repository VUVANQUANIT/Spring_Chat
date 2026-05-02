package com.Spring_chat.Web_chat.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // ── 400 Bad Request ───────────────────────────────────────────────────────
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ"),
    INVALID_JSON(HttpStatus.BAD_REQUEST, "Request body không đúng định dạng JSON"),
    MISSING_PARAMETER(HttpStatus.BAD_REQUEST, "Thiếu tham số bắt buộc"),

    // ── 401 Unauthorized ──────────────────────────────────────────────────────
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập hoặc token không hợp lệ"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Tên đăng nhập hoặc mật khẩu không đúng"),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "Refresh token không hợp lệ hoặc đã hết hạn"),

    // ── 403 Forbidden ─────────────────────────────────────────────────────────
    FORBIDDEN(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập tài nguyên này"),
    ACCOUNT_DISABLED(HttpStatus.FORBIDDEN, "Tài khoản chưa được kích hoạt"),
    ACCOUNT_BANNED(HttpStatus.FORBIDDEN, "Tài khoản của bạn đã bị cấm"),

    // ── 404 Not Found ─────────────────────────────────────────────────────────
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy tài nguyên yêu cầu"),

    // ── 409 Conflict ──────────────────────────────────────────────────────────
    USERNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "Tên đăng nhập đã được sử dụng"),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "Email đã được sử dụng"),
    FRIENDSHIP_REQUEST_EXISTS(HttpStatus.CONFLICT,"Đã gửi lời mởi kết bạn"),
    HAS_FRIEND(HttpStatus.CONFLICT,"Đã là bạn bè"),
    // 422 BUSINESS_RULE_VIOLATED
    BUSINESS_RULE_VIOLATED(HttpStatus.UNPROCESSABLE_CONTENT,"Không gửi được lời mời cho chính mình"),
    CANNOT_BLOCK_SELF(HttpStatus.UNPROCESSABLE_CONTENT,"Không thể chặn chính mình"),
    CANNOT_DELETE_SELF(HttpStatus.UNPROCESSABLE_CONTENT,"Không thể tự xóa chính mình"),
    CANNOT_INVITE_BLOCK(HttpStatus.UNPROCESSABLE_CONTENT,"Không thể mời người đã block"),
    // ── 500 Internal Server Error ─────────────────────────────────────────────
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống, vui lòng thử lại sau");

    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(HttpStatus httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }

    public String code() {
        return this.name();
    }
}
