package com.Spring_chat.Spring_chat.ENUM;

public enum PermissionName {

    // ── User management ──────────────────────────────────────────────
    USER_READ,
    USER_WRITE,
    USER_DELETE,
    USER_BAN,

    // ── Messaging ────────────────────────────────────────────────────
    MESSAGE_READ,
    MESSAGE_WRITE,
    MESSAGE_DELETE,
    MESSAGE_DELETE_ANY,

    // ── Conversations ────────────────────────────────────────────────
    CONVERSATION_READ,
    CONVERSATION_WRITE,
    CONVERSATION_DELETE,

    // ── Social / friendships ─────────────────────────────────────────
    FRIENDSHIP_READ,
    FRIENDSHIP_WRITE,

    // ── Administration ───────────────────────────────────────────────
    ADMIN_PANEL_ACCESS,
    ROLE_MANAGE
}
