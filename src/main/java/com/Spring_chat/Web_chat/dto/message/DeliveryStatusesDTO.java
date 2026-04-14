package com.Spring_chat.Web_chat.dto.message;

import com.Spring_chat.Web_chat.enums.MessageDeliveryStatus;
import lombok.Data;

import java.time.Instant;

@Data
public class DeliveryStatusesDTO {
    private Long userId;
    private MessageDeliveryStatus status;
    private Instant updatedAt;
}
