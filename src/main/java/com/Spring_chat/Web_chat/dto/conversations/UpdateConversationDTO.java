package com.Spring_chat.Web_chat.dto.conversations;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateConversationDTO {

    private Long id;

    @NotBlank(message = "Tiêu đề nhóm không được để trống")
    @Size(max = 100, message = "Tiêu đề nhóm không được vượt quá 100 ký tự")
    private String title;

    @Size(max = 500, message = "URL ảnh đại diện không được vượt quá 500 ký tự")
    private String avatarUrl;
}
