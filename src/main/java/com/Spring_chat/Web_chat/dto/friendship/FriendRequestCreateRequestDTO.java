package com.Spring_chat.Web_chat.dto.friendship;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class FriendRequestCreateRequestDTO {
    @NotNull(message = "Không được để trống")
    @Positive(message = "Yêu cầu không hợp lệ")
    private Long addresseeId;
}
