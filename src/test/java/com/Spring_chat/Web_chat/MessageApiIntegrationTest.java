package com.Spring_chat.Web_chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "jwt.secret=0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF",
        "jwt.expiration=86400000",
        "jwt.refresh-expiration=604800000"
})
@AutoConfigureMockMvc
class MessageApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private UserContext alice;
    private UserContext bob;
    private UserContext carol;
    private UserContext dave;

    @BeforeEach
    void setUp() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        alice = registerAndGetContext("alice_msg_" + suffix);
        bob = registerAndGetContext("bob_msg_" + suffix);
        carol = registerAndGetContext("carol_msg_" + suffix);
        dave = registerAndGetContext("dave_msg_" + suffix);
    }

    @Test
    void markAsRead_ValidRequest_Success() throws Exception {
        long conversationId = createGroupConversation(alice.token, "Read Team", bob.id, carol.id);
        sendTextMessage(alice.token, conversationId, "hello 1");
        long lastReadMessageId = sendTextMessage(alice.token, conversationId, "hello 2");

        mockMvc.perform(post("/api/conversations/{id}/read", conversationId)
                        .header("Authorization", "Bearer " + bob.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "lastReadMessageId": %d
                                }
                                """.formatted(lastReadMessageId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Read receipt updated"))
                .andExpect(jsonPath("$.data.conversationId").value(conversationId))
                .andExpect(jsonPath("$.data.lastReadMessageId").value(lastReadMessageId))
                .andExpect(jsonPath("$.data.unreadCount").value(0));
    }

    @Test
    void markAsRead_NotAParticipant_ReturnsForbidden() throws Exception {
        long conversationId = createGroupConversation(alice.token, "Forbidden Team", bob.id, carol.id);
        long messageId = sendTextMessage(alice.token, conversationId, "secret");

        mockMvc.perform(post("/api/conversations/{id}/read", conversationId)
                        .header("Authorization", "Bearer " + dave.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "lastReadMessageId": %d
                                }
                                """.formatted(messageId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void markAsRead_MessageNotInConversation_ReturnsError() throws Exception {
        long conversationId = createGroupConversation(alice.token, "Alpha Team", bob.id, carol.id);
        long otherConversationId = createGroupConversation(alice.token, "Beta Team", bob.id, dave.id);
        long foreignMessageId = sendTextMessage(alice.token, otherConversationId, "beta only");

        mockMvc.perform(post("/api/conversations/{id}/read", conversationId)
                        .header("Authorization", "Bearer " + bob.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "lastReadMessageId": %d
                                }
                                """.formatted(foreignMessageId)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATED"));
    }

    @Test
    void markAsRead_MessageNotFound_ReturnsNotFound() throws Exception {
        long conversationId = createGroupConversation(alice.token, "Missing Message Team", bob.id, carol.id);

        mockMvc.perform(post("/api/conversations/{id}/read", conversationId)
                        .header("Authorization", "Bearer " + bob.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "lastReadMessageId": 999999999
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    private long createGroupConversation(String token, String title, Long... participantIds) throws Exception {
        StringBuilder participantJson = new StringBuilder();
        for (int i = 0; i < participantIds.length; i++) {
            if (i > 0) {
                participantJson.append(", ");
            }
            participantJson.append(participantIds[i]);
        }

        MvcResult result = mockMvc.perform(post("/api/conversations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "GROUP",
                                  "title": "%s",
                                  "participantIds": [%s]
                                }
                                """.formatted(title, participantJson)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("data").get("id").asLong();
    }

    private long sendTextMessage(String token, long conversationId, String content) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/conversations/{id}/messages", conversationId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "TEXT",
                                  "content": "%s"
                                }
                                """.formatted(content)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("data").get("id").asLong();
    }

    private UserContext registerAndGetContext(String username) throws Exception {
        MvcResult regResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"%s",
                                  "email":"%s@mail.test",
                                  "fullName":"Full %s",
                                  "password":"Aa!123456",
                                  "confirmPassword":"Aa!123456"
                                }
                                """.formatted(username, username, username)))
                .andExpect(status().isCreated())
                .andReturn();

        String token = objectMapper.readTree(regResult.getResponse().getContentAsString()).get("access_token").asText();

        MvcResult meResult = mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        long id = objectMapper.readTree(meResult.getResponse().getContentAsString()).get("data").get("id").asLong();

        UserContext ctx = new UserContext();
        ctx.token = token;
        ctx.id = id;
        ctx.username = username;
        return ctx;
    }

    private static class UserContext {
        private String token;
        private long id;
        private String username;
    }
}
