package com.Spring_chat.Web_chat.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
public class MessageDeletedEventListener {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageDeleted(MessageDeletedEvent event) {
        log.info(
                "MESSAGE_DELETED messageId={} conversationId={} deletedBy={}",
                event.messageId(),
                event.conversationId(),
                event.deletedByUserId()
        );
    }
}
