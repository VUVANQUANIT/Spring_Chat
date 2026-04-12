package com.Spring_chat.Web_chat.dto.conversations;

import com.Spring_chat.Web_chat.enums.ConversationType;
import lombok.Data;

import java.time.Instant;

@Data
public class ConversationSummaryDTO {
  private Long id;
  private ConversationType type;
  private String title;
  private String avatarUrl;
  private Instant createdAt;
  private LastMessageDTO lastMessage;
  private int unreadCount;
  private OtherParticipantDTO otherParticipant;

}
