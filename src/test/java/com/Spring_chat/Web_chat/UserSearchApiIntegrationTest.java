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
class UserSearchApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String aliceToken;
    private Long aliceId;
    private String uniqueSuffix;

    @BeforeEach
    void setUp() throws Exception {
        uniqueSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        
        // Register Alice
        UserContext alice = registerAndGetContext("alice_" + uniqueSuffix);
        aliceToken = alice.token;
        aliceId = alice.id;
    }

    private static class UserContext {
        String token;
        Long id;
        String username;
    }

    private UserContext registerAndGetContext(String username) throws Exception {
        // 1. Register
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
        
        // 2. Get ID from /me
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
    void searchUsers_shouldMatchKeywordsAndExcludeSelf() throws Exception {
        registerAndGetContext("bob_" + uniqueSuffix);
        registerAndGetContext("carol_" + uniqueSuffix);

        mockMvc.perform(get("/api/users/search")
                        .header("Authorization", "Bearer " + aliceToken)
                        .param("q", uniqueSuffix))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[0].username").value("bob_" + uniqueSuffix))
                .andExpect(jsonPath("$.data.content[1].username").value("carol_" + uniqueSuffix));
    }

    @Test
    void searchUsers_withShortQuery_shouldReturn400() throws Exception {
        mockMvc.perform(get("/api/users/search")
                        .header("Authorization", "Bearer " + aliceToken)
                        .param("q", "a"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchUsers_shouldReflectFriendRelations() throws Exception {
        UserContext bob = registerAndGetContext("bob_rel_" + uniqueSuffix);

        // 1. NONE
        mockMvc.perform(get("/api/users/search")
                        .header("Authorization", "Bearer " + aliceToken)
                        .param("q", bob.username))
                .andExpect(jsonPath("$.data.content[0].friendRelation").value("NONE"));

        // 2. PENDING_SENT
        mockMvc.perform(post("/api/friendships/requests")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addresseeId\": %d}".formatted(bob.id)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/users/search")
                        .header("Authorization", "Bearer " + aliceToken)
                        .param("q", bob.username))
                .andExpect(jsonPath("$.data.content[0].friendRelation").value("PENDING_SENT"));

        // 3. FRIENDS
        MvcResult requestsResult = mockMvc.perform(get("/api/friendships/requests")
                        .header("Authorization", "Bearer " + bob.token)
                        .param("direction", "RECEIVED"))
                .andExpect(status().isOk())
                .andReturn();
        long friendshipId = objectMapper.readTree(requestsResult.getResponse().getContentAsString())
                .get("data").get("content").get(0).get("friendRequestId").asLong();

        mockMvc.perform(post("/api/friendships/requests/%d/accept".formatted(friendshipId))
                        .header("Authorization", "Bearer " + bob.token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users/search")
                        .header("Authorization", "Bearer " + aliceToken)
                        .param("q", bob.username))
                .andExpect(jsonPath("$.data.content[0].friendRelation").value("FRIENDS"));
    }

    @Test
    void searchUsers_shouldExcludeUsersWhoBlockedMe() throws Exception {
        UserContext bob = registerAndGetContext("bob_block_" + uniqueSuffix);

        // Bob blocks Alice
        mockMvc.perform(post("/api/friendships/%d/block".formatted(aliceId))
                        .header("Authorization", "Bearer " + bob.token))
                .andExpect(status().isOk());

        // Alice searches for Bob -> empty
        mockMvc.perform(get("/api/users/search")
                        .header("Authorization", "Bearer " + aliceToken)
                        .param("q", bob.username))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }
}
