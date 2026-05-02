package com.Spring_chat.Web_chat.dto.message;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DeliveredReceiptResponseDTO {
    private int updatedCount;
}
