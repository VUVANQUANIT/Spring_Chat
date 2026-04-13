package com.Spring_chat.Web_chat.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponseDTO {
    private String access_token;
    private String refresh_token;
    private String token_type ="Bearer";
    private String expiresIn;
    public LoginResponseDTO(String access_token, String refresh_token, String expires_in) {
        this.access_token = access_token;
        this.refresh_token = refresh_token;
        this.expiresIn = expires_in;
    }
}
