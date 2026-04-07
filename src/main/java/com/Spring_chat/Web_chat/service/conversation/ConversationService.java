package com.Spring_chat.Web_chat.service.conversation;

import com.Spring_chat.Web_chat.dto.ApiResponse;
import com.Spring_chat.Web_chat.dto.conversations.CreateConversationsDTO;
import com.Spring_chat.Web_chat.dto.conversations.CreateConversationsResponseDTO;

public interface ConversationService {
    ApiResponse<CreateConversationsResponseDTO> createConversation(CreateConversationsDTO createConversationsDTO);


}
