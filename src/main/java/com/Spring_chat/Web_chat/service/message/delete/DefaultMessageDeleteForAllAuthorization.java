package com.Spring_chat.Web_chat.service.message.delete;

import com.Spring_chat.Web_chat.entity.Message;
import com.Spring_chat.Web_chat.entity.Role;
import com.Spring_chat.Web_chat.entity.User;
import com.Spring_chat.Web_chat.enums.RoleName;
import com.Spring_chat.Web_chat.exception.AppException;
import com.Spring_chat.Web_chat.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class DefaultMessageDeleteForAllAuthorization implements MessageDeleteForAllAuthorization {

    static final Duration SENDER_DELETE_WINDOW = Duration.ofMinutes(30);

    private final Clock clock;

    @Override
    public void assertMayDeleteForAll(Message message, User actor) {
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(actor, "actor");

        if (isStaff(actor)) {
            return;
        }
        if (!Objects.equals(message.getSender().getId(), actor.getId())) {
            throw new AppException(ErrorCode.FORBIDDEN, "Chỉ người gửi mới được xóa tin cho mọi người");
        }
        Instant deadline = message.getCreatedAt().plus(SENDER_DELETE_WINDOW);
        if (Instant.now(clock).isAfter(deadline)) {
            throw new AppException(ErrorCode.BUSINESS_RULE_VIOLATED, "Hết thời gian cho phép xóa tin cho mọi người (30 phút)");
        }
    }

    private static boolean isStaff(User user) {
        if (user.getRoles() == null) {
            return false;
        }
        return user.getRoles().stream()
                .map(Role::getName)
                .anyMatch(r -> r == RoleName.ROLE_ADMIN || r == RoleName.ROLE_MODERATOR);
    }
}
