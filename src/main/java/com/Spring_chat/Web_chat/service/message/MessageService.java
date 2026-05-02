package com.Spring_chat.Web_chat.service.message;

import com.Spring_chat.Web_chat.dto.ApiResponse;
import com.Spring_chat.Web_chat.dto.message.DeliveredReceiptRequestDTO;
import com.Spring_chat.Web_chat.dto.message.DeliveredReceiptResponseDTO;
import com.Spring_chat.Web_chat.dto.message.MessageListResponseDTO;
import com.Spring_chat.Web_chat.dto.message.ReadReceiptRequestDTO;
import com.Spring_chat.Web_chat.dto.message.ReadReceiptResponseDTO;
import com.Spring_chat.Web_chat.dto.message.SendMessageRequestDTO;
import com.Spring_chat.Web_chat.dto.message.SendMessageResponseDTO;
import com.Spring_chat.Web_chat.dto.message.UpdateMessageRequestDTO;
import com.Spring_chat.Web_chat.dto.message.UpdateMessageResponseDTO;
import com.Spring_chat.Web_chat.enums.MessageDeleteScope;

public interface MessageService {
    ApiResponse<MessageListResponseDTO> getMessageList(Long beforeId, Integer limit,Long conversationId);
    ApiResponse<SendMessageResponseDTO> sendMessage(Long conversationId, SendMessageRequestDTO request);
    ApiResponse<UpdateMessageResponseDTO> updateMessage(Long messageId, UpdateMessageRequestDTO request);
    void deleteMessage(long messageId, MessageDeleteScope scope);
    ApiResponse<DeliveredReceiptResponseDTO> markAsDelivered(DeliveredReceiptRequestDTO request);
    ApiResponse<ReadReceiptResponseDTO> markAsRead(Long conversationId, ReadReceiptRequestDTO request);
    void invalidateParticipantCache(Long conversationId, Long userId);
}
