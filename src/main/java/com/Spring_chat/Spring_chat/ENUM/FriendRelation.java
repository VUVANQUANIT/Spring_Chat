package com.Spring_chat.Spring_chat.ENUM;

/**
 * Trạng thái quan hệ giữa currentUser và một user khác trong kết quả search.
 * Dùng để FE biết hiển thị nút "Kết bạn" / "Đang chờ" / "Đã là bạn".
 */
public enum FriendRelation {
    /** Chưa có quan hệ gì */
    NONE,
    /** Đã là bạn bè */
    FRIENDS,
    /** Mình đã gửi lời mời, đang chờ họ chấp nhận */
    PENDING_SENT,
    /** Họ đã gửi lời mời cho mình, đang chờ mình xử lý */
    PENDING_RECEIVED
}
