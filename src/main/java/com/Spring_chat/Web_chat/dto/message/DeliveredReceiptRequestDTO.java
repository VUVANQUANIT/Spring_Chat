package com.Spring_chat.Web_chat.dto.message;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.List;

@Data
public class DeliveredReceiptRequestDTO {

    @NotEmpty(message = "messageIds is required")
    private List<@NotNull @Positive Long> messageIds;
}
