package com.Spring_chat.Web_chat.dto.user;

/**
 * Spring Data JPA projection interface cho native search query.
 *
 * Các getter phải khớp (case-insensitive) với alias trong SQL SELECT:
 *   id, username, fullName, avatarUrl, friendRelation
 */
public interface UserSearchProjection {
    Long   getId();
    String getUsername();
    String getFullName();
    String getAvatarUrl();
    /** Giá trị trả về là string: NONE / FRIENDS / PENDING_SENT / PENDING_RECEIVED */
    String getFriendRelation();
}
