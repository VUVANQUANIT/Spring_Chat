package com.Spring_chat.Web_chat.dto.message;

import lombok.Data;

import java.util.List;

@Data
public class MessageListResponseDTO {
    private List<MessageSummaryDTO> items;
    private boolean hasMore;
}
