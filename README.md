# Spring Chat - Entity Model

## Tổng quan

Bộ entity này được tạo từ schema SQL bạn cung cấp, gồm 6 bảng chính:

- `User`
- `Friendship`
- `Conversation`
- `ConversationParticipant`
- `Message`
- `MessageStatus`

Các entity nằm trong package:

- `src/main/java/com/Spring_chat/Spring_chat/entity`
- `src/main/java/com/Spring_chat/Spring_chat/ENUM`

## Mô tả từng entity

### 1) `User`

Đại diện tài khoản người dùng trong hệ thống chat.

Thuộc tính chính:

- `id`: khóa chính.
- `username`, `email`: duy nhất (`UNIQUE`).
- `passwordHash`: mật khẩu đã hash.
- `fullName`, `avatarUrl`: thông tin hồ sơ.
- `status`: trạng thái tài khoản (`ACTIVE`, `INACTIVE`, `BANNED`).
- `lastSeen`: thời điểm online gần nhất.
- `createdAt`, `updatedAt`: thời điểm tạo/cập nhật.

### 2) `Friendship`

Đại diện mối quan hệ kết bạn giữa 2 user.

Thuộc tính chính:

- `requester`: user gửi lời mời.
- `addressee`: user nhận lời mời.
- `status`: trạng thái kết bạn (`PENDING`, `ACCEPTED`, `REJECTED`, `BLOCKED`).
- `createdAt`, `updatedAt`.

Ràng buộc:

- Duy nhất cặp `(requesterId, addresseeId)`.
- Không cho phép tự kết bạn (`requesterId != addresseeId`).

### 3) `Conversation`

Đại diện một cuộc hội thoại.

Thuộc tính chính:

- `type`: loại hội thoại (`PRIVATE`, `GROUP`).
- `createdAt`.

### 4) `ConversationParticipant`

Đại diện thành viên tham gia hội thoại.

Thuộc tính chính:

- `conversation`: hội thoại tham gia.
- `user`: người tham gia.
- `joinedAt`: thời điểm tham gia.
- `leftAt`: thời điểm rời nhóm (nếu có).
- `lastReadMessage`: message gần nhất user đã đọc.

Ràng buộc:

- Duy nhất cặp `(conversationId, userId)`.

### 5) `Message`

Đại diện tin nhắn trong hội thoại.

Thuộc tính chính:

- `conversation`: hội thoại chứa tin nhắn.
- `sender`: người gửi.
- `content`: nội dung.
- `type`: loại tin nhắn (`TEXT`, `IMAGE`, `FILE`, `VOICE`, `SYSTEM`).
- `replyTo`: liên kết message được reply.
- `isDeleted`: đánh dấu xóa mềm.
- `createdAt`.

### 6) `MessageStatus`

Đại diện trạng thái gửi/nhận/đã xem của 1 message theo từng user.

Thuộc tính chính:

- `message`: tin nhắn.
- `user`: người nhận trạng thái.
- `status`: (`SENT`, `DELIVERED`, `SEEN`).
- `updatedAt`.

Ràng buộc:

- Duy nhất cặp `(messageId, userId)`.

## Quan hệ chính

- `Friendship.requester` -> `User` (`ManyToOne`)
- `Friendship.addressee` -> `User` (`ManyToOne`)
- `ConversationParticipant.conversation` -> `Conversation` (`ManyToOne`)
- `ConversationParticipant.user` -> `User` (`ManyToOne`)
- `ConversationParticipant.lastReadMessage` -> `Message` (`ManyToOne`)
- `Message.conversation` -> `Conversation` (`ManyToOne`)
- `Message.sender` -> `User` (`ManyToOne`)
- `Message.replyTo` -> `Message` (`ManyToOne`, self reference)
- `MessageStatus.message` -> `Message` (`ManyToOne`)
- `MessageStatus.user` -> `User` (`ManyToOne`)

## Ghi chú triển khai

- Các cột thời gian dùng `Instant` để map với `TIMESTAMP WITH TIME ZONE`.
- Đã dùng `@CreationTimestamp` và `@UpdateTimestamp` để tự cập nhật thời gian ở tầng ORM.
- Trạng thái (`status`, `type`) được map bằng `enum` và lưu dạng chuỗi (`EnumType.STRING`).
