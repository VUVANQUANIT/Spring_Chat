package com.Spring_chat.Web_chat;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

    // ── Helpers shared across tests ──────────────────────────────────────────

    private String registerUser(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("User-Agent", "AuthApiIntegrationTest")
                        .content("""
                                {
                                  "username":"%s",
                                  "email":"%s@mail.test",
                                  "password":"%s",
                                  "confirmPassword":"%s"
                                }
                                """.formatted(username, username, password, password)))
                .andExpect(status().isCreated())
                .andReturn();
        return result.getResponse().getContentAsString();
    }

    private void assertErrorShape(String errorCode) throws Exception {
        // Used as a reminder: all error responses must contain these fields
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void authFlow_registerLoginRefreshLogout_refreshAfterLogoutShouldFail() throws Exception {
        String username = "user_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String password = "Aa!123456";

        String registerJson = registerUser(username, password);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("User-Agent", "AuthApiIntegrationTest")
                        .content("""
                                {
                                  "username":"%s",
                                  "email":"%s@mail.test",
                                  "password":"%s",
                                  "confirmPassword":"%s"
                                }
                                """.formatted(username, username, password, password)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USERNAME_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.status").value(409));

        String firstAccessToken = extractJsonString(registerJson, "access_token");
        String firstRefreshToken = extractJsonString(registerJson, "refresh_token");

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

        // After logout, refresh should fail with standard error format
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refresh_token":"%s"
                                }
                                """.formatted(rotatedRefreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.path").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    // ── Error format tests ────────────────────────────────────────────────────

    @Test
    void register_withDuplicateUsername_shouldReturn409WithCode() throws Exception {
        String username = "dup_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String password = "Aa!123456";

        registerUser(username, password);

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
                .andExpect(jsonPath("$.code").value("USERNAME_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.path").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void login_withWrongPassword_shouldReturn401WithCode() throws Exception {
        String username = "login_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String password = "Aa!123456";

        registerUser(username, password);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"%s",
                                  "password":"Wrong!123"
                                }
                                """.formatted(username)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.path").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void refresh_withInvalidToken_shouldReturn401WithCode() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refresh_token":"invalid-token"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.path").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void register_withValidationErrors_shouldReturn400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"ab",
                                  "email":"not-an-email",
                                  "password":"weak",
                                  "confirmPassword":"weak"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.path").isNotEmpty());
    }

    @Test
    void register_withInvalidJson_shouldReturn400WithCode() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ invalid json }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_JSON"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void updateMyProfile_shouldReturnUpdatedProfileDto() throws Exception {
        String username = "profile_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String password = "Aa!123456";

        String registerJson = registerUser(username, password);
        String accessToken = extractJsonString(registerJson, "access_token");

        mockMvc.perform(patch("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName":"Nguyen Van A",
                                  "avatarUrl":"https://cdn.example.com/avatar.png"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Profile updated successfully"))
                .andExpect(jsonPath("$.data.username").value(username))
                .andExpect(jsonPath("$.data.email").value(username + "@mail.test"))
                .andExpect(jsonPath("$.data.fullName").value("Nguyen Van A"))
                .andExpect(jsonPath("$.data.avatarUrl").value("https://cdn.example.com/avatar.png"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void updateMyProfile_withInvalidAvatarUrl_shouldReturn400WithFieldErrors() throws Exception {
        String username = "profile_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String password = "Aa!123456";

        String registerJson = registerUser(username, password);
        String accessToken = extractJsonString(registerJson, "access_token");

        mockMvc.perform(patch("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "avatarUrl":"not-a-url"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors").isNotEmpty());
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private static String extractJsonString(String json, String fieldName) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) {
            throw new AssertionError("Field not found in JSON response: " + fieldName);
        }
        return matcher.group(1);
    }
}
