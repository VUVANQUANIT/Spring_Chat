package com.Spring_chat.Web_chat.dto.conversations;

import lombok.Data;

import java.time.Instant;
import java.util.List;
@Data
public class ConversationListDTO {
   private List<ConversationSummaryDTO> items;
   private Instant nextCursor;
   boolean hasMore;
}
