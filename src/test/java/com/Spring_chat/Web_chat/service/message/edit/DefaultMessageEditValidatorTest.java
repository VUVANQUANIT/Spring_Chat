package com.Spring_chat.Web_chat.service.message.edit;

import com.Spring_chat.Web_chat.entity.Message;
import com.Spring_chat.Web_chat.entity.User;
import com.Spring_chat.Web_chat.enums.MessageType;
import com.Spring_chat.Web_chat.exception.AppException;
import com.Spring_chat.Web_chat.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultMessageEditValidatorTest {

    private static final Instant CREATED = Instant.parse("2026-01-15T10:00:00Z");

    @Test
    void assertEditable_WithinWindowAndDifferentContent_Succeeds() {
        Clock clock = Clock.fixed(Instant.parse("2026-01-15T10:29:59Z"), ZoneOffset.UTC);
        DefaultMessageEditValidator validator = new DefaultMessageEditValidator(clock);
        User actor = User.builder().id(1L).username("alice").build();
        Message message = baseTextMessage(actor, "hello");

        assertDoesNotThrow(() -> validator.assertEditable(message, actor, "hello!"));
    }

    @Test
    void assertEditable_ExactlyAtDeadline_Succeeds() {
        Clock clock = Clock.fixed(CREATED.plus(DefaultMessageEditValidator.EDIT_WINDOW), ZoneOffset.UTC);
        DefaultMessageEditValidator validator = new DefaultMessageEditValidator(clock);
        User actor = User.builder().id(1L).build();
        Message message = baseTextMessage(actor, "a");

        assertDoesNotThrow(() -> validator.assertEditable(message, actor, "b"));
    }

    @Test
    void assertEditable_AfterDeadline_ThrowsBusinessRule() {
        Clock clock = Clock.fixed(CREATED.plus(DefaultMessageEditValidator.EDIT_WINDOW).plusSeconds(1), ZoneOffset.UTC);
        DefaultMessageEditValidator validator = new DefaultMessageEditValidator(clock);
        User actor = User.builder().id(1L).build();
        Message message = baseTextMessage(actor, "a");

        AppException ex = assertThrows(AppException.class, () -> validator.assertEditable(message, actor, "b"));
        assertEquals(ErrorCode.BUSINESS_RULE_VIOLATED, ex.getErrorCode());
    }

    @Test
    void assertEditable_NotSender_ThrowsForbidden() {
        Clock clock = Clock.fixed(Instant.parse("2026-01-15T10:15:00Z"), ZoneOffset.UTC);
        DefaultMessageEditValidator validator = new DefaultMessageEditValidator(clock);
        User sender = User.builder().id(1L).build();
        User intruder = User.builder().id(2L).build();
        Message message = baseTextMessage(sender, "secret");

        AppException ex = assertThrows(AppException.class, () -> validator.assertEditable(message, intruder, "x"));
        assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
    }

    @Test
    void assertEditable_DeletedMessage_ThrowsBusinessRule() {
        Clock clock = Clock.fixed(Instant.parse("2026-01-15T10:15:00Z"), ZoneOffset.UTC);
        DefaultMessageEditValidator validator = new DefaultMessageEditValidator(clock);
        User actor = User.builder().id(1L).build();
        Message message = baseTextMessage(actor, "a");
        message.setIsDeleted(true);

        AppException ex = assertThrows(AppException.class, () -> validator.assertEditable(message, actor, "b"));
        assertEquals(ErrorCode.BUSINESS_RULE_VIOLATED, ex.getErrorCode());
    }

    @Test
    void assertEditable_NonTextType_ThrowsBusinessRule() {
        Clock clock = Clock.fixed(Instant.parse("2026-01-15T10:15:00Z"), ZoneOffset.UTC);
        DefaultMessageEditValidator validator = new DefaultMessageEditValidator(clock);
        User actor = User.builder().id(1L).build();
        Message message = baseTextMessage(actor, "https://example.com/a.png");
        message.setType(MessageType.IMAGE);

        AppException ex = assertThrows(AppException.class, () -> validator.assertEditable(message, actor, "other"));
        assertEquals(ErrorCode.BUSINESS_RULE_VIOLATED, ex.getErrorCode());
    }

    @Test
    void assertEditable_SameContent_ThrowsBusinessRule() {
        Clock clock = Clock.fixed(Instant.parse("2026-01-15T10:15:00Z"), ZoneOffset.UTC);
        DefaultMessageEditValidator validator = new DefaultMessageEditValidator(clock);
        User actor = User.builder().id(1L).build();
        Message message = baseTextMessage(actor, "same");

        AppException ex = assertThrows(AppException.class, () -> validator.assertEditable(message, actor, "same"));
        assertEquals(ErrorCode.BUSINESS_RULE_VIOLATED, ex.getErrorCode());
    }

    private static Message baseTextMessage(User sender, String content) {
        return Message.builder()
                .sender(sender)
                .type(MessageType.TEXT)
                .isDeleted(false)
                .content(content)
                .createdAt(CREATED)
                .build();
    }
}
