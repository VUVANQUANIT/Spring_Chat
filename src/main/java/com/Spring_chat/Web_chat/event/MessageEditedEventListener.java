package com.Spring_chat.Web_chat.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Placeholder for realtime fan-out (e.g. STOMP {@code /topic/conversations/{id}}).
 * Keeps {@link com.Spring_chat.Web_chat.service.message.MessageService} free of transport concerns.
 */
@Component
@Slf4j
public class MessageEditedEventListener {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageEdited(MessageEditedEvent event) {
        log.info(
                "MESSAGE_EDITED messageId={} conversationId={} editedBy={}",
                event.messageId(),
                event.conversationId(),
                event.editedByUserId()
        );
        log.debug("MESSAGE_EDITED payload content length={}", event.content() != null ? event.content().length() : 0);
    }
}
