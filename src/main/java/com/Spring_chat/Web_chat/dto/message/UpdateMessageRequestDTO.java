package com.Spring_chat.Web_chat.dto.message;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateMessageRequestDTO {

    @NotBlank(message = "Nội dung không được để trống")
    @Size(max = 4000, message = "Nội dung tối đa 4000 ký tự")
    private String content;
}
