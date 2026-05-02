# Plan Thực Thi: API Đánh Dấu Đã Đọc Tin Nhắn (Read Receipt)

**Tài liệu dành cho AI/Cursor thực thi việc lập trình.**

---

## 1. Tổng quan yêu cầu

Theo `CHAT_API_SPEC_DETAILED.md` (Section 6.1), hệ thống cần xây dựng API cho phép người dùng đánh dấu đã đọc (SEEN) tới một tin nhắn cụ thể trong cuộc hội thoại.

*   **Endpoint:** `POST /api/conversations/{id}/read`
*   **Mục tiêu:** 
    1. Cập nhật `lastReadMessage` của người dùng trong cuộc hội thoại.
    2. Cập nhật hàng loạt (bulk update) trạng thái của các `MessageStatus` liên quan sang trạng thái `SEEN`.
    3. Trả về thông tin cập nhật (bao gồm số tin nhắn chưa đọc còn lại - sẽ được tính toán).
    4. (Optional/Future) Bắn event realtime qua WebSocket.

---

## 2. Các thành phần cần tạo/chỉnh sửa

### 2.1. Request & Response DTOs
Tạo các class DTO trong package `com.Spring_chat.Web_chat.dto.message` (hoặc `dto.conversations`):

**`ReadReceiptRequestDTO.java`**
```java
package com.Spring_chat.Web_chat.dto.message;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReadReceiptRequestDTO {
    @NotNull(message = "lastReadMessageId is required")
    private Long lastReadMessageId;
}
```

**`ReadReceiptResponseDTO.java`**
```java
package com.Spring_chat.Web_chat.dto.message;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReadReceiptResponseDTO {
    private Long conversationId;
    private Long lastReadMessageId;
    private int unreadCount;
}
```

### 2.2. Repository (Query cập nhật trạng thái)

Trong `MessageDeliveryStatusRepo.java` (hoặc `MessageStatusRepository.java` tùy tên thực tế trong project), cần thêm các custom query sau:

1. **Cập nhật trạng thái tin nhắn thành SEEN:**
```java
@Modifying
@Query("UPDATE MessageStatus ms SET ms.status = 'SEEN', ms.updatedAt = CURRENT_TIMESTAMP " +
       "WHERE ms.user.id = :userId " +
       "AND ms.status != 'SEEN' " +
       "AND ms.message.id IN (" +
       "   SELECT m.id FROM Message m WHERE m.conversation.id = :conversationId AND m.id <= :lastReadMessageId" +
       ")")
int updateStatusToSeenForUserAndConversation(
        @Param("userId") Long userId, 
        @Param("conversationId") Long conversationId, 
        @Param("lastReadMessageId") Long lastReadMessageId
);
```

2. **Đếm số tin nhắn chưa đọc (unread count):**
```java
@Query("SELECT COUNT(ms) FROM MessageStatus ms " +
       "WHERE ms.user.id = :userId " +
       "AND ms.message.conversation.id = :conversationId " +
       "AND ms.status != 'SEEN' " +
       "AND ms.message.isDeleted = false") // tùy thuộc logic dự án có loại trừ tin nhắn bị xoá không
int countUnreadMessages(@Param("userId") Long userId, @Param("conversationId") Long conversationId);
```
*(Nếu câu đếm phía trên đã tồn tại trong repository, hãy sử dụng nó).*

### 2.3. Service Logic

Thêm phương thức vào `MessageService` hoặc `ConversationService`. Đề xuất thêm vào **`MessageService.java`**:

**Các bước logic chính (`markAsRead`):**
1. Gọi `CurrentUserProvider.findCurrentUserOrThrow()` để lấy `userId` hiện tại.
2. Kiểm tra `ConversationParticipant` theo `conversationId` và `userId`.
    * Nếu không tìm thấy hoặc `leftAt != null` -> throw `AppException(ErrorCode.FORBIDDEN, "Bạn không có quyền hoặc đã rời nhóm")`.
3. Kiểm tra `Message` theo `lastReadMessageId`.
    * Nếu không tồn tại -> throw `AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy tin nhắn")`.
    * Kiểm tra xem `message.getConversation().getId()` có khớp với `conversationId` từ path param hay không. Nếu không khớp -> throw `AppException(ErrorCode.BUSINESS_RULE_VIOLATED, "Tin nhắn không thuộc cuộc hội thoại này")`.
4. Lấy thực thể `ConversationParticipant` ở bước 2, gán `setLastReadMessage(message)` và lưu lại qua `ConversationParticipantRepository`.
5. Gọi hàm bulk update `@Modifying` ở bước 2.2: `updateStatusToSeenForUserAndConversation(...)`.
6. Tính `unreadCount` bằng cách gọi hàm count hoặc trả về `0` (nếu logic count phức tạp, tham khảo lại BA Spec 6.3 - unreadCount thường trả về sau khi markAsRead là 0 nếu client truyền đúng tin nhắn cuối cùng).
7. Trả về `ReadReceiptResponseDTO`.

### 2.4. Controller

Cập nhật `MessageController.java` (đang có `@RequestMapping("/api/conversations")`) hoặc `ConversationController.java`:

```java
@PostMapping("/{id}/read")
public ResponseEntity<ApiResponse<ReadReceiptResponseDTO>> markAsRead(
        @PathVariable("id") Long id,
        @Valid @RequestBody ReadReceiptRequestDTO request
) {
    ReadReceiptResponseDTO response = messageService.markAsRead(id, request);
    return ResponseEntity.ok(ApiResponse.<ReadReceiptResponseDTO>builder()
            .success(true)
            .message("Read receipt updated")
            .data(response)
            .build()); // Đảm bảo dùng builder/utils chuẩn của dự án cho ApiResponse
}
```

---

## 3. Test Cases Cần Viết (Integration Test)

Cursor cần tự động bổ sung test vào file `MessageApiIntegrationTest.java` hoặc `ConversationApiIntegrationTest.java`.

Các case cần test bằng `MockMvc`:

| Tên Test Method | Mô tả logic | Expected Status |
| :--- | :--- | :--- |
| `markAsRead_ValidRequest_Success` | Gửi request hợp lệ với `lastReadMessageId` thuộc về nhóm, user là thành viên hợp lệ. | `200 OK` |
| `markAsRead_NotAParticipant_ReturnsForbidden` | User không nằm trong conversation. | `403 FORBIDDEN` |
| `markAsRead_MessageNotInConversation_ReturnsError`| `lastReadMessageId` thuộc về một conversation khác. | `422 BUSINESS_RULE_VIOLATED` |
| `markAsRead_MessageNotFound_ReturnsNotFound` | Truyền một ID tin nhắn không tồn tại. | `404 RESOURCE_NOT_FOUND` |

---

## 4. Chú ý nghiêm ngặt cho AI (Cursor)

* **Tuân thủ Layer:** KHÔNG viết logic query hay validate business vào Controller. Toàn bộ nằm ở Service layer.
* **Quy tắc JSON:** Tất cả các properties trong DTO phải giữ chuẩn `camelCase`. `ApiResponse` phải tuân theo cấu trúc dự án.
* **Transactional:** Method Service thực thi chức năng này bắt buộc phải có annotation `@Transactional` do chứa câu lệnh `@Modifying` query cập nhật DB.
* **Thời gian múi giờ:** Sử dụng `java.time.Instant` hoặc timestamp mặc định của framework, DB sẽ quản lý chuẩn UTC.