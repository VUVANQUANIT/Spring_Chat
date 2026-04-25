package com.Spring_chat.Web_chat.controller.message;

import com.Spring_chat.Web_chat.dto.ApiResponse;
import com.Spring_chat.Web_chat.dto.message.MessageListResponseDTO;
import com.Spring_chat.Web_chat.dto.message.SendMessageRequestDTO;
import com.Spring_chat.Web_chat.dto.message.SendMessageResponseDTO;
import jakarta.validation.Valid;
import com.Spring_chat.Web_chat.service.message.MessageService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
@Validated
public class MessageController {

    private final MessageService messageService;

    /**
     * GET /api/conversations/{id}/messages
     * 
     * Lấy danh sách tin nhắn của một cuộc hội thoại với phân trang cursor.
     * @param id ID của conversation
     * @param beforeId Lấy các tin nhắn cũ hơn ID này (scroll up)
     * @param limit Số lượng tối đa muốn lấy (default 30, max 100)
     */
    @GetMapping("/{id}/messages")
    public ResponseEntity<ApiResponse<MessageListResponseDTO>> getConversationMessages(
            @PathVariable("id") Long id,
            @RequestParam(required = false) Long beforeId,
            @RequestParam(defaultValue = "30", required = false) @Min(1) @Max(100) Integer limit) {
        
        return ResponseEntity.ok(messageService.getMessageList(beforeId, limit, id));
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<ApiResponse<SendMessageResponseDTO>> sendMessage(
            @PathVariable("id") Long id,
            @Valid @RequestBody SendMessageRequestDTO request
    ) {
        return ResponseEntity.status(201).body(messageService.sendMessage(id, request));
    }
}

