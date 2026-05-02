package com.Spring_chat.Web_chat.controller.message;

import com.Spring_chat.Web_chat.dto.ApiResponse;
import com.Spring_chat.Web_chat.dto.message.DeliveredReceiptRequestDTO;
import com.Spring_chat.Web_chat.dto.message.DeliveredReceiptResponseDTO;
import com.Spring_chat.Web_chat.dto.message.UpdateMessageRequestDTO;
import com.Spring_chat.Web_chat.dto.message.UpdateMessageResponseDTO;
import com.Spring_chat.Web_chat.enums.MessageDeleteScope;
import com.Spring_chat.Web_chat.service.message.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@Validated
public class MessageCommandController {

    private final MessageService messageService;

    /**
     * PATCH /api/messages/{id} — sửa nội dung tin TEXT (trong 30 phút), chỉ sender.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<UpdateMessageResponseDTO>> updateMessage(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateMessageRequestDTO request) {
        return ResponseEntity.ok(messageService.updateMessage(id, request));
    }

    /**
     * DELETE /api/messages/{id}?scope=ME|ALL — ẩn cho bản thân hoặc xóa mềm cho mọi người.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable("id") Long id,
            @RequestParam("scope") String scope) {
        messageService.deleteMessage(id, MessageDeleteScope.fromQueryParam(scope));
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/messages/delivered — client ack message delivery after receiving data.
     */
    @PostMapping("/delivered")
    public ResponseEntity<ApiResponse<DeliveredReceiptResponseDTO>> markAsDelivered(
            @Valid @RequestBody DeliveredReceiptRequestDTO request) {
        return ResponseEntity.ok(messageService.markAsDelivered(request));
    }
}
