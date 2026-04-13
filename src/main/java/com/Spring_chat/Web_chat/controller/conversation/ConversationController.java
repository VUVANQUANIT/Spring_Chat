package com.Spring_chat.Web_chat.controller.conversation;

import com.Spring_chat.Web_chat.dto.ApiResponse;
import com.Spring_chat.Web_chat.dto.conversations.ConversationDetailDTO;
import com.Spring_chat.Web_chat.dto.conversations.ConversationListDTO;
import com.Spring_chat.Web_chat.dto.conversations.CreateConversationsDTO;
import com.Spring_chat.Web_chat.dto.conversations.CreateConversationsResponseDTO;
import com.Spring_chat.Web_chat.service.conversation.ConversationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    @PostMapping
    public ResponseEntity<ApiResponse<CreateConversationsResponseDTO>> createConversation(
            @Valid @RequestBody CreateConversationsDTO dto) {
        return ResponseEntity.status(201).body(conversationService.createConversation(dto));
    }

    /**
     * GET /api/conversations
     *
     * Query params (Spec 4.2):
     *   cursor : ISO-8601 timestamp — sort-time của item cuối ở trang trước (omit for first page)
     *   limit  : số lượng, default 20, max 50
     */
    @GetMapping
    public ResponseEntity<ApiResponse<ConversationListDTO>> getUserConversations(
            @RequestParam(required = false)          String cursor,
            @RequestParam(defaultValue = "20")       int    limit) {

        int clampedLimit = Math.min(Math.max(limit, 1), 50);
        return ResponseEntity.ok(
                conversationService.getUserConversation(PageRequest.of(0, clampedLimit), cursor)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ConversationDetailDTO>> getConversationDetail(@PathVariable("id") Long id) {
        return ResponseEntity.ok(conversationService.getConversationDetail(id));
    }
}
