package com.Spring_chat.Web_chat.dto.message;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReadReceiptRequestDTO {
    @NotNull(message = "lastReadMessageId is required")
    private Long lastReadMessageId;
}
