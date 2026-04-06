# 🔐 Security Architecture & RBAC Flow

Tài liệu này giải thích chi tiết về cơ chế bảo mật, phân quyền và xác thực trong **Spring Chat**.

---

## 🏗️ Kiến trúc Stateless
Hệ thống được thiết kế theo hướng **Stateless**, không lưu trữ Session trên Server. Điều này giúp hệ thống dễ dàng mở rộng (Scale-out) theo chiều ngang.

### 🔑 JSON Web Token (JWT)
Chúng tôi sử dụng 2 loại token:
1. **Access Token**: Có thời gian sống ngắn (ví dụ: 15-30 phút). Chứa danh sách các `Roles` và `Permissions`.
2. **Refresh Token**: Có thời gian sống dài (ví dụ: 7 ngày). Được lưu trong Database để quản lý phiên đăng nhập và hỗ trợ thu hồi quyền truy cập (Revocation).

---

## 🛡️ Hệ thống phân quyền (RBAC + Permission)

Hệ thống kết hợp giữa **Role** (Nhóm quyền) và **Permission** (Hành vi cụ thể).

### 1. Phân quyền đa lớp
- **URL-Level Security**: Quy định quyền truy cập dựa trên tiền tố URL (e.g., `/api/admin/**` chỉ dành cho `ROLE_ADMIN`).
- **Method-Level Security**: Sử dụng `@PreAuthorize` để kiểm soát hành vi cụ thể (e.g., `@PreAuthorize("hasAuthority('MESSAGE_DELETE_ANY')")`).

### 2. Các Role mặc định
- `ROLE_USER`: Người dùng cơ bản, có quyền chat và quản lý profile cá nhân.
- `ROLE_MODERATOR`: Có quyền xóa tin nhắn vi phạm, quản lý báo cáo người dùng.
- `ROLE_ADMIN`: Quyền hạn tối cao, quản lý cấu hình hệ thống và phân quyền.

---

## 🔄 Refresh Token Flow (Luồng xoay vòng Token)

Để bảo mật tối đa, chúng tôi áp dụng cơ chế **Refresh Token Rotation**:
1. Khi Access Token hết hạn, User gửi Refresh Token lên Server.
2. Server kiểm tra tính hợp lệ của Refresh Token:
   - Nếu hợp lệ: Cấp **cả Access Token mới và Refresh Token mới**, đồng thời vô hiệu hóa Refresh Token cũ.
   - Nếu phát hiện Refresh Token cũ đã bị sử dụng lại (Reuse Detection): Vô hiệu hóa toàn bộ các token của User đó (đề phòng trường hợp bị hacker chiếm đoạt).

---

## 🛡️ Các biện pháp bảo mật khác
- **Password Hashing**: Sử dụng `BCrypt` với độ mạnh (strength) được cấu hình chuẩn.
- **Traceability**: Mỗi request đều được gán một `traceId` để dễ dàng tra cứu log khi có sự cố.
- **XSS & CSRF Protection**: Được cấu hình mặc định bởi Spring Security cho môi trường Stateless.

---
*Tài liệu này được biên soạn cho các chuyên gia bảo mật và developer cần hiểu sâu về hệ thống.*
