# 📖 API Specification & Integration Guide

Tài liệu này cung cấp chi tiết về các Endpoint, yêu cầu xác thực và cấu trúc dữ liệu trả về của hệ thống **Spring Chat**.

---

## 🔐 Authentication & Security

Hệ thống sử dụng **JWT (JSON Web Token)** stateless.
- **Base URL**: `http://localhost:8080/api/v1`
- **Header**: `Authorization: Bearer <access_token>`

### Luồng xác thực (Authentication Flow)
1. **Login**: Gọi `POST /auth/login` để nhận `access_token` và `refresh_token`.
2. **Accessing API**: Đính kèm `access_token` vào Header cho mọi request.
3. **Token Expired**: Khi Access Token hết hạn (401 Unauthorized), gọi `POST /auth/refresh` kèm `refresh_token` để lấy cặp token mới.
4. **Logout**: Gọi `POST /auth/logout` để thu hồi toàn bộ refresh token trên mọi thiết bị.

---

## 📡 Core API Endpoints

### 1. User & Profile
- `GET /users/me`: Lấy thông tin tài khoản hiện tại.
- `PATCH /users/me`: Cập nhật Profile (`fullName`, `avatarUrl`).
- `GET /users/search?q={query}`: Tìm kiếm người dùng (Hỗ trợ Postgres Trigram Search).

### 2. Friendship (Quan hệ bạn bè)
- `POST /friendships/requests`: Gửi lời mời kết bạn.
- `GET /friendships/requests?status=PENDING`: Danh sách lời mời đang chờ.
- `POST /friendships/requests/{id}/accept`: Chấp nhận kết bạn.
- `DELETE /friendships/{userId}`: Hủy kết bạn/Chặn người dùng.

### 3. Conversations & Messaging
- `GET /conversations`: Lấy danh sách các cuộc hội thoại (Private/Group).
- `GET /conversations/{id}/messages`: Tải lịch sử tin nhắn (Hỗ trợ Pagination với Cursor).
- `POST /conversations/{id}/messages`: Gửi tin nhắn mới.

---

## ⚠️ Error Handling (Chuẩn xử lý lỗi)

Mọi lỗi trả về từ hệ thống đều tuân thủ cấu trúc sau:

```json
{
  "code": "ERROR_CODE_NAME",
  "status": 400,
  "message": "Mô tả lỗi chi tiết cho developer",
  "timestamp": "2026-04-06T10:00:00Z",
  "path": "/api/v1/endpoint",
  "traceId": "unique-request-uuid"
}
```

### Các mã lỗi phổ biến:
- `VALIDATION_FAILED`: Dữ liệu đầu vào không hợp lệ (kiểm tra trường `errors`).
- `UNAUTHORIZED`: Token không hợp lệ hoặc đã hết hạn.
- `ACCESS_DENIED`: Bạn không có đủ Permission để thực hiện hành động này.
- `RESOURCE_NOT_FOUND`: Không tìm thấy dữ liệu yêu cầu.

---

## 🚀 WebSocket Events (Real-time)

**Endpoint**: `ws://localhost:8080/ws`

| Topic | Sự kiện |
| :--- | :--- |
| `/topic/messages/{conversationId}` | Nhận tin nhắn mới trong hội thoại |
| `/user/queue/notifications` | Nhận thông báo riêng (Lời mời kết bạn, v.v.) |
| `/topic/presence` | Trạng thái Online/Offline của bạn bè |

---
*Lưu ý: Luôn kiểm tra `traceId` khi báo lỗi với đội ngũ Backend.*
