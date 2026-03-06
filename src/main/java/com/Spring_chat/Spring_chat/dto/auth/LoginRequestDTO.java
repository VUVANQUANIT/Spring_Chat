package com.Spring_chat.Spring_chat.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequestDTO {
    @NotBlank(message = "Tên đăng nhập không được để trống")
    @Size(max = 50, message = "Tên đăng nhập quá dài")
    private String username;
    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 1,max = 100,message = "Mật khẩu quá dài")
    private String password;
}
