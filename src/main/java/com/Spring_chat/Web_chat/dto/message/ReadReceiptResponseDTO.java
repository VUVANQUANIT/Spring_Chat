package com.Spring_chat.Web_chat.dto.message;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReadReceiptResponseDTO {
    private Long conversationId;
    
    // ID của tin nhắn mới nhất vừa được đánh dấu là đã đọc (SEEN)
    private Long lastReadMessageId;
    
    // Số lượng tin nhắn CÒN LẠI chưa đọc trong conversation này sau khi đã markAsRead
    private long unreadCount;
}
