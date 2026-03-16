# Chat API & Flow Spec (Upgrade from Legacy)

Ngày cập nhật: 2026-03-13

Tài liệu này mô tả:
1) Hiện trạng có thể làm được gì ngay với code đang có.
2) Danh sách API cần thiết để nâng cấp thành chat realtime hoàn chỉnh.
3) Luồng nghiệp vụ chi tiết để FE/BE thống nhất triển khai.

---

## 1) Hiện trạng có thể làm được ngay

Dựa trên code hiện có, bạn đã sẵn sàng cho:
- Authentication + JWT:
    - `POST /api/auth/register`
    - `POST /api/auth/login`
    - `POST /api/auth/refresh`
    - `POST /api/auth/logout`
- RBAC + Permission: đã seed Role/Permission và cấu hình Spring Security.
- Chuẩn hóa lỗi API + traceId.
- Entity data model cho chat đã có đầy đủ bảng cốt lõi:
    - `User`, `Friendship`, `Conversation`, `ConversationParticipant`, `Message`, `MessageStatus`.

Những phần CHƯA có nên chưa làm được ngay:
- Không có controller/service/repository cho chat.
- Không có WebSocket/STOMP config.
- Không có upload ảnh.
- Không có cơ chế “xóa cho tôi” (ẩn theo user).
- Không có rule edit/delete 30 phút trong entity.

---

## 2) Danh sách API cần thiết

### 2.1. User
- `GET /api/users/me` lấy profile bản thân.
- `PATCH /api/users/me` cập nhật `fullName`, `avatarUrl`.
- `GET /api/users/{id}` xem profile user khác.
- `GET /api/users/search?q=` tìm kiếm user.

### 2.2. Friendship
- `POST /api/friendships/requests` gửi lời mời `{addresseeId}`.
- `GET /api/friendships/requests?status=PENDING|ACCEPTED|REJECTED|BLOCKED`.
- `POST /api/friendships/requests/{id}/accept`.
- `POST /api/friendships/requests/{id}/reject`.
- `POST /api/friendships/{userId}/block`.
- `DELETE /api/friendships/{userId}` unfriend.

### 2.3. Conversation
- `POST /api/conversations` tạo hội thoại.
    - Body: `{ type: PRIVATE|GROUP, participantIds[], title?, avatarUrl? }`
- `GET /api/conversations?cursor=&limit=` danh sách hội thoại.
- `GET /api/conversations/{id}` chi tiết hội thoại + participants.
- `PATCH /api/conversations/{id}` đổi tên/ảnh nhóm (nếu GROUP).
- `POST /api/conversations/{id}/participants` thêm người (GROUP).
- `DELETE /api/conversations/{id}/participants/{userId}` rời/kick.

### 2.4. Message
- `GET /api/conversations/{id}/messages?beforeId=&limit=` tải lịch sử.
- `POST /api/conversations/{id}/messages` gửi tin nhắn.
    - Body: `{ content, type: TEXT|IMAGE, replyToId?, clientMessageId? }`
- `PATCH /api/messages/{id}` sửa tin nhắn (<= 30 phút).
- `DELETE /api/messages/{id}?scope=ME|ALL`
    - `ME`: xóa cho tôi (ẩn theo user)
    - `ALL`: xóa cho tất cả (<= 30 phút)

### 2.5. Read/Delivery Status
- `POST /api/conversations/{id}/read` `{ lastReadMessageId }`
- `POST /api/messages/delivered` `{ messageIds[] }`
- `GET /api/conversations/{id}/unread-count`

### 2.6. Upload Image
- `POST /api/uploads/images` trả `{ url, width, height, size }`

### 2.7. Realtime (WebSocket / STOMP)
- Endpoint: `/ws`
- Subscribe:
    - `/topic/conversations/{id}` message mới / update
    - `/user/queue/messages` message riêng
    - `/topic/typing/{conversationId}` typing
    - `/topic/presence` online/offline
- Send:
    - `/app/messages.send`
    - `/app/messages.read`
    - `/app/messages.edit`
    - `/app/messages.delete`
    - `/app/typing`

---

## 3) Luồng nghiệp vụ chi tiết

### 3.1. Luồng đăng ký / đăng nhập
1. User `POST /api/auth/register`.
2. Backend trả access + refresh token.
3. FE lưu access token, setup refresh.
4. Khi access token hết hạn, gọi `POST /api/auth/refresh`.

### 3.2. Luồng gửi tin nhắn
1. FE gọi `POST /api/conversations/{id}/messages`.
2. Backend:
    - Check user là participant.
    - Tạo `Message`.
    - Tạo `MessageStatus` cho từng participant (SENT/DELIVERED tuỳ realtime).
3. Backend push realtime lên `/topic/conversations/{id}`.

### 3.3. Luồng nhận tin nhắn realtime
1. Client subscribe `/topic/conversations/{id}`.
2. Khi có message mới, render ngay.
3. Client gửi `/app/messages.read` hoặc REST `POST /api/conversations/{id}/read`.

### 3.4. Luồng sửa tin nhắn (<= 30 phút)
1. Client gọi `PATCH /api/messages/{id}`.
2. Backend validate:
    - Sender là người sửa.
    - `now <= createdAt + 30 phút`.
3. Update `content`, set `editedAt`.
4. Push realtime event “message edited”.

### 3.5. Luồng xóa cho tôi
1. Client gọi `DELETE /api/messages/{id}?scope=ME`.
2. Backend lưu `MessageHidden(userId, messageId)`.
3. FE chỉ ẩn message ở user đó.

### 3.6. Luồng xóa cho tất cả (<= 30 phút)
1. Client gọi `DELETE /api/messages/{id}?scope=ALL`.
2. Backend validate:
    - Sender hoặc moderator.
    - `now <= createdAt + 30 phút` (trừ moderator nếu cho phép override).
3. Set `Message.isDeleted=true`, `deletedAt`, `deletedBy`.
4. Push realtime event “message deleted”.

### 3.7. Luồng read receipt
1. Client scroll đến message cuối.
2. Gọi `POST /api/conversations/{id}/read` với `lastReadMessageId`.
3. Backend update:
    - `ConversationParticipant.lastReadMessage`.
    - `MessageStatus` của user → SEEN.
4. Push realtime event “read receipt”.

### 3.8. Luồng typing
1. Client emit `/app/typing` khi gõ.
2. Backend relay tới `/topic/typing/{conversationId}`.

### 3.9. Luồng presence (online/offline)
1. Khi kết nối WS, backend mark user online.
2. Khi disconnect, update `lastSeen`.
3. Push `/topic/presence`.

---

## 4) Bổ sung DB tối thiểu cần có

Để đáp ứng yêu cầu:
- `Message.editedAt`, `Message.editedBy`, `isEdited`
- `Message.deletedAt`, `Message.deletedBy`
- Bảng `MessageHidden` (xóa cho tôi)
- (Tuỳ giai đoạn) `Conversation.title`, `Conversation.avatarUrl`, `ownerId`

---

## 5) Gợi ý ưu tiên triển khai
1. REST: Conversations + Messages + Read receipt.
2. WebSocket realtime (message new + read + edit + delete).
3. Xóa cho tôi + xóa cho tất cả.
4. Upload image.
5. Presence + typing.

---

## 6) Ghi chú áp dụng từ hệ thống cũ

- Nếu hệ thống cũ chỉ có 1-1, giữ `Conversation.type = PRIVATE`.
- Khi nâng cấp nhóm, thêm metadata và endpoints participant.
- Không phá vỡ API cũ: có thể giữ `/api/messages` và bổ sung `conversationId` param.

---

Tài liệu này là baseline để bạn triển khai backend. Nếu cần mình sẽ tách thành API spec chi tiết từng request/response sau.