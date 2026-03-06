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

## Hệ thống phân quyền (RBAC + Permission)

### Tổng quan

Hệ thống sử dụng mô hình RBAC (Role-Based Access Control) kết hợp permission chi tiết, với JWT stateless:

- `Role` đại diện nhóm quyền (User, Moderator, Admin).
- `Permission` mô tả hành vi cụ thể (đọc/ghi/xóa user, message, conversation, v.v.).
- Mỗi `User` có tập `Role`; mỗi `Role` ánh xạ tới nhiều `Permission` qua bảng nối `role_permission`.
- Khi login, hệ thống build tập `GrantedAuthority` từ cả `Role` và `Permission`, sau đó embed vào JWT để không phải query DB mỗi request.

### Entity và enum liên quan

- `Role` (`src/main/java/com/Spring_chat/Spring_chat/entity/Role.java`)
  - Trường chính:
    - `name: RoleName` (enum, lưu dạng chuỗi).
    - `description: String` (mô tả role).
    - `permissions: Set<Permission>` (many-to-many).

- `Permission` (`src/main/java/com/Spring_chat/Spring_chat/entity/Permission.java`)
  - Trường chính:
    - `name: PermissionName` (enum, lưu dạng chuỗi).
    - `description: String` (mô tả permission).

- `RoleName` (`src/main/java/com/Spring_chat/Spring_chat/ENUM/RoleName.java`)
  - Các giá trị chính (authority dạng `"ROLE_*"`):
    - `ROLE_USER`
    - `ROLE_MODERATOR`
    - `ROLE_ADMIN`

- `PermissionName` (`src/main/java/com/Spring_chat/Spring_chat/ENUM/PermissionName.java`)
  - Nhóm **quản lý user**:
    - `USER_READ`, `USER_WRITE`, `USER_DELETE`, `USER_BAN`
  - Nhóm **tin nhắn**:
    - `MESSAGE_READ`, `MESSAGE_WRITE`, `MESSAGE_DELETE`, `MESSAGE_DELETE_ANY`
  - Nhóm **hội thoại**:
    - `CONVERSATION_READ`, `CONVERSATION_WRITE`, `CONVERSATION_DELETE`
  - Nhóm **quan hệ bạn bè**:
    - `FRIENDSHIP_READ`, `FRIENDSHIP_WRITE`
  - Nhóm **admin / hệ thống**:
    - `ADMIN_PANEL_ACCESS`, `ROLE_MANAGE`

### Cách Spring Security sử dụng Role / Permission

- Adapter `UserDetailsImpl` (`src/main/java/com/Spring_chat/Spring_chat/security/UserDetailsImpl.java`):
  - Được dùng lúc authenticate (login) để chuyển entity `User` sang `UserDetails`.
  - Hàm `buildAuthorities` tạo ra tập `GrantedAuthority` từ:
    - Tên role (ví dụ: `ROLE_ADMIN`, `ROLE_USER`).
    - Các permission gắn với role (ví dụ: `MESSAGE_DELETE_ANY`, `USER_BAN`).

- Cấu hình `SecurityConfig` (`src/main/java/com/Spring_chat/Spring_chat/security/SecurityConfig.java`):
  - Dùng `@EnableWebSecurity` và `@EnableMethodSecurity(prePostEnabled = true)`.
  - Quy tắc URL-level:
    - `/api/auth/**` và `/error` → `permitAll()`.
    - `/api/admin/**` → `hasRole("ADMIN")` (tương ứng authority `ROLE_ADMIN`).
    - `/api/moderation/**` → `hasAnyRole("ADMIN", "MODERATOR")`.
    - Các request khác → `authenticated()`; phân quyền chi tiết thực hiện bằng annotation ở method (ví dụ `@PreAuthorize`).

### JWT và principal trong request

- `JwtService` (`src/main/java/com/Spring_chat/Spring_chat/security/JwtService.java`):
  - Sinh access token với các claim:
    - `sub` (subject): username.
    - `uid`: user id (Long).
    - `roles`: danh sách authority (role/permission) dạng `List<String>`.
  - Thời gian sống được cấu hình qua `JwtConfig`.

- `JwtAuthenticationFilter` (`src/main/java/com/Spring_chat/Spring_chat/security/JwtAuthenticationFilter.java`):
  - Chạy cho mỗi request (stateless).
  - Đọc header `Authorization: Bearer <token>`:
    - Validate token và extract `Claims` qua `JwtService`.
    - Lấy `uid`, `sub`, và danh sách `roles` (authority string).
    - Convert thành `Collection<GrantedAuthority>` và gán vào `SecurityContext` mà **không cần truy vấn DB**.

- `AuthenticatedUser` (`src/main/java/com/Spring_chat/Spring_chat/security/AuthenticatedUser.java`):
  - Principal nhẹ được tạo từ JWT claims.
  - Có sẵn:
    - `id`: user id.
    - `username`.
    - `authorities`: danh sách role/permission dưới dạng `GrantedAuthority`.
  - Hỗ trợ helper:
    - `hasRole(String role)`
    - `hasPermission(String permission)`
  - Có thể inject vào controller qua `@AuthenticationPrincipal AuthenticatedUser principal` để check quyền ở tầng API/业务.

### Cách mở rộng phân quyền

- **Thêm permission mới**:
  - Thêm giá trị mới vào `PermissionName`.
  - Tạo record tương ứng trong bảng `permission` (seed hoặc migration).
  - Gán vào các role phù hợp qua bảng `role_permission`.
  - Dùng trong code với `@PreAuthorize("hasAuthority('PERMISSION_NAME_MOI')")` hoặc `principal.hasPermission("PERMISSION_NAME_MOI")`.

- **Thêm role mới**:
  - Thêm vào enum `RoleName` (ví dụ: `ROLE_SUPPORT`).
  - Tạo record trong bảng `role`.
  - Mapping các permission phù hợp cho role mới.
  - Cập nhật rule trong `SecurityConfig` (nếu cần phân biệt theo URL prefix) hoặc dùng `@PreAuthorize("hasRole('ROLE_SUPPORT')")` / `hasAnyRole(...)`.

- **Ưu điểm**:
  - Stateless, dễ scale: không lưu session server-side, chỉ dựa vào JWT.
  - Tách bạch rõ `Role` (nhóm quyền) và `Permission` (hành vi cụ thể).
  - Dễ thay đổi mapping role ↔ permission trong DB mà không phải sửa code.
