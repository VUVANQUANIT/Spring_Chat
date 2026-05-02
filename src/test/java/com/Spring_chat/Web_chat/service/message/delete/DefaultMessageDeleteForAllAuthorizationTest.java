package com.Spring_chat.Web_chat.service.message.delete;

import com.Spring_chat.Web_chat.entity.Message;
import com.Spring_chat.Web_chat.entity.Role;
import com.Spring_chat.Web_chat.entity.User;
import com.Spring_chat.Web_chat.enums.MessageType;
import com.Spring_chat.Web_chat.enums.RoleName;
import com.Spring_chat.Web_chat.exception.AppException;
import com.Spring_chat.Web_chat.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultMessageDeleteForAllAuthorizationTest {

    private static final Instant CREATED = Instant.parse("2026-03-01T10:00:00Z");

    @Test
    void senderWithinWindow_Allows() {
        Clock clock = Clock.fixed(CREATED.plus(DefaultMessageDeleteForAllAuthorization.SENDER_DELETE_WINDOW)
                .minusSeconds(1), ZoneOffset.UTC);
        var auth = new DefaultMessageDeleteForAllAuthorization(clock);
        User sender = User.builder().id(5L).build();
        Message message = messageFromSender(sender);

        assertDoesNotThrow(() -> auth.assertMayDeleteForAll(message, sender));
    }

    @Test
    void senderAfterWindow_Denies() {
        Clock clock = Clock.fixed(CREATED.plus(DefaultMessageDeleteForAllAuthorization.SENDER_DELETE_WINDOW)
                .plusSeconds(1), ZoneOffset.UTC);
        var auth = new DefaultMessageDeleteForAllAuthorization(clock);
        User sender = User.builder().id(5L).build();
        Message message = messageFromSender(sender);

        AppException ex = assertThrows(AppException.class, () -> auth.assertMayDeleteForAll(message, sender));
        assertEquals(ErrorCode.BUSINESS_RULE_VIOLATED, ex.getErrorCode());
    }

    @Test
    void nonSender_Denies() {
        Clock clock = Clock.fixed(CREATED.plusSeconds(60), ZoneOffset.UTC);
        var auth = new DefaultMessageDeleteForAllAuthorization(clock);
        User sender = User.builder().id(1L).build();
        User other = User.builder().id(2L).build();
        Message message = messageFromSender(sender);

        AppException ex = assertThrows(AppException.class, () -> auth.assertMayDeleteForAll(message, other));
        assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
    }

    @Test
    void moderatorMayDeleteOthers_AfterWindow() {
        Clock clock = Clock.fixed(CREATED.plus(DefaultMessageDeleteForAllAuthorization.SENDER_DELETE_WINDOW)
                .plusSeconds(3600), ZoneOffset.UTC);
        var auth = new DefaultMessageDeleteForAllAuthorization(clock);
        User sender = User.builder().id(1L).build();
        User mod = User.builder()
                .id(99L)
                .roles(Set.of(Role.builder().name(RoleName.ROLE_MODERATOR).build()))
                .build();
        Message message = messageFromSender(sender);

        assertDoesNotThrow(() -> auth.assertMayDeleteForAll(message, mod));
    }

    @Test
    void adminMayDeleteOthers_AfterWindow() {
        Clock clock = Clock.fixed(CREATED.plus(30, ChronoUnit.DAYS), ZoneOffset.UTC);
        var auth = new DefaultMessageDeleteForAllAuthorization(clock);
        User sender = User.builder().id(1L).build();
        User admin = User.builder()
                .id(100L)
                .roles(Set.of(Role.builder().name(RoleName.ROLE_ADMIN).build()))
                .build();
        Message message = messageFromSender(sender);

        assertDoesNotThrow(() -> auth.assertMayDeleteForAll(message, admin));
    }

    private static Message messageFromSender(User sender) {
        return Message.builder()
                .sender(sender)
                .type(MessageType.TEXT)
                .content("hello")
                .createdAt(CREATED)
                .isDeleted(false)
                .build();
    }
}
