package com.Spring_chat.Web_chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.Spring_chat.Web_chat.entity.Message;
import com.Spring_chat.Web_chat.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class MessageApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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

    @Test
    void updateMessage_SenderWithinWindow_Success() throws Exception {
        long conversationId = createGroupConversation(alice.token, "Edit Team", bob.id, carol.id);
        long messageId = sendTextMessage(alice.token, conversationId, "original");

        mockMvc.perform(patch("/api/messages/{id}", messageId)
                        .header("Authorization", "Bearer " + alice.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "content": "edited body" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Message updated"))
                .andExpect(jsonPath("$.data.id").value(messageId))
                .andExpect(jsonPath("$.data.conversationId").value(conversationId))
                .andExpect(jsonPath("$.data.content").value("edited body"))
                .andExpect(jsonPath("$.data.isEdited").value(true))
                .andExpect(jsonPath("$.data.isDeleted").value(false))
                .andExpect(jsonPath("$.data.editedBy.id").value(alice.id))
                .andExpect(jsonPath("$.data.editedBy.username").value(alice.username))
                .andExpect(jsonPath("$.data.sender.id").value(alice.id));
    }

    @Test
    void updateMessage_NotSender_ReturnsForbidden() throws Exception {
        long conversationId = createGroupConversation(alice.token, "Edit Perm Team", bob.id, carol.id);
        long messageId = sendTextMessage(alice.token, conversationId, "alice only");

        mockMvc.perform(patch("/api/messages/{id}", messageId)
                        .header("Authorization", "Bearer " + bob.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "content": "bob tries" }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void updateMessage_NotParticipant_ReturnsForbidden() throws Exception {
        long conversationId = createGroupConversation(alice.token, "Outsider Team", bob.id, carol.id);
        long messageId = sendTextMessage(alice.token, conversationId, "secret");

        mockMvc.perform(patch("/api/messages/{id}", messageId)
                        .header("Authorization", "Bearer " + dave.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "content": "hack" }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void updateMessage_ImageType_ReturnsUnprocessable() throws Exception {
        long conversationId = createGroupConversation(alice.token, "Img Team", bob.id, carol.id);
        long messageId = sendImageMessage(alice.token, conversationId, "https://example.com/pic.png");

        mockMvc.perform(patch("/api/messages/{id}", messageId)
                        .header("Authorization", "Bearer " + alice.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "content": "nope" }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATED"));
    }

    @Test
    void updateMessage_SameContent_ReturnsUnprocessable() throws Exception {
        long conversationId = createGroupConversation(alice.token, "Same Team", bob.id, carol.id);
        long messageId = sendTextMessage(alice.token, conversationId, "unchanged");

        mockMvc.perform(patch("/api/messages/{id}", messageId)
                        .header("Authorization", "Bearer " + alice.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "content": "unchanged" }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATED"));
    }

    @Test
    void updateMessage_OutsideEditWindow_ReturnsUnprocessable() throws Exception {
        long conversationId = createGroupConversation(alice.token, "Stale Team", bob.id, carol.id);
        long messageId = sendTextMessage(alice.token, conversationId, "old");
        Instant oldCreated = Instant.now().minusSeconds(31 * 60);
        jdbcTemplate.update("UPDATE messages SET created_at = ? WHERE id = ?", Timestamp.from(oldCreated), messageId);

        mockMvc.perform(patch("/api/messages/{id}", messageId)
                        .header("Authorization", "Bearer " + alice.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "content": "too late" }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATED"));
    }

    @Test
    void updateMessage_DeletedMessage_ReturnsUnprocessable() throws Exception {
        long conversationId = createGroupConversation(alice.token, "Del Team", bob.id, carol.id);
        long messageId = sendTextMessage(alice.token, conversationId, "gone");
        Message m = messageRepository.findById(messageId).orElseThrow();
        m.setIsDeleted(true);
        messageRepository.save(m);

        mockMvc.perform(patch("/api/messages/{id}", messageId)
                        .header("Authorization", "Bearer " + alice.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "content": "revive" }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATED"));
    }

    @Test
    void updateMessage_BlankContent_ReturnsBadRequest() throws Exception {
        long conversationId = createGroupConversation(alice.token, "Blank Team", bob.id, carol.id);
        long messageId = sendTextMessage(alice.token, conversationId, "x");

        mockMvc.perform(patch("/api/messages/{id}", messageId)
                        .header("Authorization", "Bearer " + alice.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "content": "   " }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void updateMessage_UnknownId_ReturnsNotFound() throws Exception {
        createGroupConversation(alice.token, "Nf Team", bob.id, carol.id);

        mockMvc.perform(patch("/api/messages/{id}", 999_999_999L)
                        .header("Authorization", "Bearer " + alice.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "content": "x" }
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

    private long sendImageMessage(String token, long conversationId, String imageUrl) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/conversations/{id}/messages", conversationId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "IMAGE",
                                  "content": "%s"
                                }
                                """.formatted(imageUrl)))
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
