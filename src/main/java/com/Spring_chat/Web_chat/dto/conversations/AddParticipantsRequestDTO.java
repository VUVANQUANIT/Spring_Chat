package com.Spring_chat.Web_chat.dto.conversations;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddParticipantsRequestDTO {
    @NotEmpty(message = "Danh sách người dùng không được để trống")
    private Long[] userIds;
}
