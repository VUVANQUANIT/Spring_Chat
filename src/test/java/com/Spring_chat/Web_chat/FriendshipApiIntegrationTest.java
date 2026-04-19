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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "jwt.secret=0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF",
        "jwt.expiration=86400000",
        "jwt.refresh-expiration=604800000"
})
@AutoConfigureMockMvc
class FriendshipApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String uniqueSuffix;
    private UserContext alice;
    private UserContext bob;

    @BeforeEach
    void setUp() throws Exception {
        uniqueSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        alice = registerAndGetContext("alice_" + uniqueSuffix);
        bob = registerAndGetContext("bob_" + uniqueSuffix);
    }

    private static class UserContext {
        String token;
        Long id;
        String username;
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
        
        Long id = objectMapper.readTree(meResult.getResponse().getContentAsString()).get("data").get("id").asLong();
        
        UserContext ctx = new UserContext();
        ctx.token = token;
        ctx.id = id;
        ctx.username = username;
        return ctx;
    }

    @Test
    void fullFriendshipFlow_shouldWorkCorrectly() throws Exception {
        // 1. Alice sends request to Bob
        MvcResult requestResult = mockMvc.perform(post("/api/friendships/requests")
                        .header("Authorization", "Bearer " + alice.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addresseeId\": %d}".formatted(bob.id)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn();
        
        long requestId = objectMapper.readTree(requestResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        // 2. Bob accepts request
        mockMvc.perform(post("/api/friendships/requests/%d/accept".formatted(requestId))
                        .header("Authorization", "Bearer " + bob.token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));

        // 3. Verify conversation created (automatically)
        mockMvc.perform(get("/api/conversations")
                        .header("Authorization", "Bearer " + alice.token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].type").value("PRIVATE"))
                .andExpect(jsonPath("$.data.items[0].otherParticipant.userId").value(bob.id));

        // 4. Alice unfriends Bob
        mockMvc.perform(delete("/api/friendships/%d".formatted(bob.id))
                        .header("Authorization", "Bearer " + alice.token))
                .andExpect(status().isNoContent());

        // 5. Verify no longer friends
        mockMvc.perform(get("/api/friendships/requests")
                        .header("Authorization", "Bearer " + alice.token)
                        .param("status", "ACCEPTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    void blockUser_shouldPreventFriendRequests() throws Exception {
        // Alice blocks Bob
        mockMvc.perform(post("/api/friendships/%d/block".formatted(bob.id))
                        .header("Authorization", "Bearer " + alice.token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.friendshipStatus").value("BLOCKED"));

        // Bob tries to send request to Alice -> should fail with 404 (hidden)
        mockMvc.perform(post("/api/friendships/requests")
                        .header("Authorization", "Bearer " + bob.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addresseeId\": %d}".formatted(alice.id)))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectRequest_shouldWork() throws Exception {
        // Alice sends request to Bob
        MvcResult requestResult = mockMvc.perform(post("/api/friendships/requests")
                        .header("Authorization", "Bearer " + alice.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addresseeId\": %d}".formatted(bob.id)))
                .andExpect(status().isCreated())
                .andReturn();
        
        long requestId = objectMapper.readTree(requestResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        // Bob rejects
        mockMvc.perform(post("/api/friendships/requests/%d/reject".formatted(requestId))
                        .header("Authorization", "Bearer " + bob.token))
                .andExpect(status().isOk());

        // Verify status is REJECTED
        mockMvc.perform(get("/api/friendships/requests")
                        .header("Authorization", "Bearer " + bob.token)
                        .param("status", "REJECTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }
}
