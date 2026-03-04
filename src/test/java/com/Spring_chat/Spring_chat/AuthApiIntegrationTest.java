package com.Spring_chat.Spring_chat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "jwt.secret=0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF",
        "jwt.expiration=86400000",
        "jwt.refresh-expiration=604800000"
})
@AutoConfigureMockMvc
class AuthApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void authFlow_registerLoginRefreshLogout_refreshAfterLogoutShouldFail() throws Exception {
        String username = "user_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String email = username + "@mail.test";
        String password = "Aa!123456";

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("User-Agent", "AuthApiIntegrationTest")
                        .content("""
                                {
                                  "username":"%s",
                                  "email":"%s",
                                  "password":"%s",
                                  "confirmPassword":"%s"
                                }
                                """.formatted(username, email, password, password)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.access_token").isNotEmpty())
                .andExpect(jsonPath("$.refresh_token").isNotEmpty())
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").isNotEmpty())
                .andReturn();

        String firstAccessToken = extractJsonString(registerResult.getResponse().getContentAsString(), "access_token");
        String firstRefreshToken = extractJsonString(registerResult.getResponse().getContentAsString(), "refresh_token");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("User-Agent", "AuthApiIntegrationTest")
                        .content("""
                                {
                                  "username":"%s",
                                  "password":"%s"
                                }
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").isNotEmpty())
                .andExpect(jsonPath("$.refresh_token").isNotEmpty())
                .andExpect(jsonPath("$.token_type").value("Bearer"));

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("User-Agent", "AuthApiIntegrationTest")
                        .content("""
                                {
                                  "refresh_token":"%s"
                                }
                                """.formatted(firstRefreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").isNotEmpty())
                .andExpect(jsonPath("$.refresh_token").isNotEmpty())
                .andReturn();

        String rotatedRefreshToken = extractJsonString(refreshResult.getResponse().getContentAsString(), "refresh_token");

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + firstAccessToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refresh_token":"%s"
                                }
                                """.formatted(rotatedRefreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void register_withDuplicateUsername_shouldReturnConflict() throws Exception {
        String username = "dup_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String password = "Aa!123456";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"%s",
                                  "email":"%s@mail.test",
                                  "password":"%s",
                                  "confirmPassword":"%s"
                                }
                                """.formatted(username, username, password, password)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"%s",
                                  "email":"%s_2@mail.test",
                                  "password":"%s",
                                  "confirmPassword":"%s"
                                }
                                """.formatted(username, username, password, password)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void login_withWrongPassword_shouldReturnUnauthorized() throws Exception {
        String username = "login_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String password = "Aa!123456";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"%s",
                                  "email":"%s@mail.test",
                                  "password":"%s",
                                  "confirmPassword":"%s"
                                }
                                """.formatted(username, username, password, password)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"%s",
                                  "password":"Wrong!123"
                                }
                                """.formatted(username)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void refresh_withInvalidToken_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refresh_token":"invalid-token"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    private static String extractJsonString(String json, String fieldName) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) {
            throw new AssertionError("Field not found in JSON response: " + fieldName);
        }
        return matcher.group(1);
    }
}
