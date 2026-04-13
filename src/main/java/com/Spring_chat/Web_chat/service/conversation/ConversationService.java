package com.Spring_chat.Web_chat.service.conversation;

import com.Spring_chat.Web_chat.dto.ApiResponse;
import com.Spring_chat.Web_chat.dto.conversations.*;
import org.springframework.data.domain.Pageable;



public interface ConversationService {
    ApiResponse<CreateConversationsResponseDTO> createConversation(CreateConversationsDTO createConversationsDTO);
    ApiResponse<ConversationListDTO>  getUserConversation(Pageable pageable, String cursor);
    ApiResponse<ConversationDetailDTO> getConversationDetail(Long conversationId);
    ApiResponse<UpdateConversationDTO> updateConversation(Long id, UpdateConversationDTO updateConversationDTO);
    ApiResponse<ListUserDTO> addUserToConversation(Long conversationId, ListUserDTO listUserDTO);
}
