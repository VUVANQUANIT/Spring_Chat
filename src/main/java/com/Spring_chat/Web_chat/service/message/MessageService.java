package com.Spring_chat.Web_chat.service.message;

import com.Spring_chat.Web_chat.dto.ApiResponse;
import com.Spring_chat.Web_chat.dto.message.MessageListResponseDTO;

public interface MessageService {
    ApiResponse<MessageListResponseDTO> getMessageList(Long beforeId, Integer limit,Long conversationId);
}
