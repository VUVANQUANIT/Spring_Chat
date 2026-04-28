package com.Spring_chat.Web_chat.dto.message;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReadReceiptResponseDTO {
    private Long conversationId;
    private Long lastReadMessageId;
    private int unreadCount;
}
