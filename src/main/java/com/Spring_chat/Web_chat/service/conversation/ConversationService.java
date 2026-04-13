package com.Spring_chat.Web_chat.service.conversation;

import com.Spring_chat.Web_chat.dto.ApiResponse;
import com.Spring_chat.Web_chat.dto.conversations.ConversationDetailDTO;
import com.Spring_chat.Web_chat.dto.conversations.ConversationListDTO;
import com.Spring_chat.Web_chat.dto.conversations.CreateConversationsDTO;
import com.Spring_chat.Web_chat.dto.conversations.CreateConversationsResponseDTO;
import org.springframework.data.domain.Pageable;



public interface ConversationService {
    ApiResponse<CreateConversationsResponseDTO> createConversation(CreateConversationsDTO createConversationsDTO);
    ApiResponse<ConversationListDTO>  getUserConversation(Pageable pageable, String cursor);
    ApiResponse<ConversationDetailDTO> getConversationDetail(Long conversationId);
}
