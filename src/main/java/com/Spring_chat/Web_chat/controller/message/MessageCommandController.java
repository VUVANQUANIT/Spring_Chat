package com.Spring_chat.Web_chat.controller.message;

import com.Spring_chat.Web_chat.dto.ApiResponse;
import com.Spring_chat.Web_chat.dto.message.UpdateMessageRequestDTO;
import com.Spring_chat.Web_chat.dto.message.UpdateMessageResponseDTO;
import com.Spring_chat.Web_chat.service.message.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
