package com.Spring_chat.Web_chat.dto.message;

import com.Spring_chat.Web_chat.enums.MessageDeliveryStatus;
import com.Spring_chat.Web_chat.enums.MessageType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class MessageSummaryDTO {
    private Long id;
    private Long conversationId;
    private SenderDTO sender;
    private String content;
    private MessageType type;
    private Long replyTo; // Can be a nested DTO (like ReplyMessageDTO) or just the ID. Using Long for now.
    
    @JsonProperty("isDeleted")
    private boolean isDeleted;
    
    @JsonProperty("isEdited")
    private boolean isEdited;
    
    private Instant editedAt;
    private Instant createdAt;
    
    private MessageDeliveryStatus myStatus; // Use Enum, not Entity
    private List<DeliveryStatusesDTO> deliveryStatuses; // Must be a list
}
