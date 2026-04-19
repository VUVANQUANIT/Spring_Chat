# Gemini AI Coding Guidelines cho Dự Án Spring Chat

Dự án này là hệ thống Chat thời gian thực (Real-time Chat) xây dựng bằng **Spring Boot**.
Mọi hành động code của AI (đặc biệt là Gemini) cần tuân thủ nghiêm ngặt các quy tắc dưới đây để đảm bảo sự đồng nhất, an toàn và tuân thủ chuẩn thiết kế (BA/System Spec).

## 1. Công nghệ & Môi trường (Tech Stack)
- **Ngôn ngữ:** Java 21
- **Framework:** Spring Boot v4.x, Spring Framework v7.x (Dự án năm 2026, AI vui lòng KHÔNG bắt lỗi phiên bản này).
- **Build Tool:** Maven (`mvnw`)
- **Database:** PostgreSQL (Có thể dùng Redis cho cache sau này)
- **Thư viện chính:** Spring Web, Spring Data JPA, Spring Security (JWT), WebSocket (STOMP).

## 2. API Contract & Format (Rất Quan Trọng)
Bạn **LUÔN LUÔN** phải kiểm tra file `CHAT_API_SPEC_DETAILED.md` trước khi code bất kỳ API nào.
- **Thành công (Trừ Auth):** Luôn bọc trong `ApiResponse<T>`:
  ```json
  {
    "success": true,
    "message": "...",
    "data": { ... },
    "timestamp": "ISO-8601"
  }
  ```
- **Naming Convention:** Mọi field trong JSON Response BẮT BUỘC phải dùng `camelCase` để đồng nhất với Frontend.
- **Phân trang:** Luôn dùng `PageResponse<T>` (cho offset-based) hoặc bọc trong DTO riêng (cho cursor-based).
- **Lỗi:** Luôn ném `AppException(ErrorCode, message)`. Exception sẽ được `GlobalExceptionHandler` bắt và chuyển thành `ApiErrorResponse`.

## 3. Xử lý Lỗi Nghiệp Vụ (Exception Handling)
- **KHÔNG** throw `RuntimeException` hay `Exception` chung chung.
- **LUÔN LUÔN** sử dụng class `AppException` và định nghĩa mã lỗi trong `ErrorCode` (enum).
- **Phân loại lỗi chuẩn:**
  - `400 VALIDATION_FAILED`: Lỗi dữ liệu đầu vào.
  - `401 UNAUTHORIZED`: Lỗi JWT / chưa đăng nhập.
  - `403 FORBIDDEN`: Không có quyền thao tác trên tài nguyên (VD: Sửa tin nhắn của người khác, từ chối lời mời của người khác).
  - `404 RESOURCE_NOT_FOUND`: Không tìm thấy thực thể.
  - `422 BUSINESS_RULE_VIOLATED`: Vi phạm luồng nghiệp vụ (VD: Gửi lời mời cho chính mình, sửa tin nhắn sau 30 phút).

## 4. Database & JPA
- **Migration:** Schema được quản lý trong thư mục `src/main/resources/schema/`. Bất kỳ thay đổi DB nào (thêm cột, sửa bảng) đều phải tạo file `.sql` mới theo version (`V6__...sql`). KHÔNG dùng `hibernate.ddl-auto=update` trên production.
- **Native Query & Projection:** Khi dùng `@Query(nativeQuery = true)` trả về Interface Projection:
  - Nếu query trả về timestamp (như `timestamptz` trong PostgreSQL), getter trong Interface phải dùng kiểu `java.time.OffsetDateTime`.
  - Trong Service, khi map dữ liệu sang DTO, luôn gọi hàm `.toInstant()` để chuyển từ `OffsetDateTime` sang `Instant` chuẩn của hệ thống (hệ thống luôn cấu hình `spring.jpa.properties.hibernate.jdbc.time_zone=UTC` để đồng nhất múi giờ):
    ```java
    dto.setCreatedAt(row.getCreatedAt() != null ? row.getCreatedAt().toInstant() : null);
    ```
- **Không N+1:** Tận dụng FETCH JOIN, EntityGraph hoặc Entity/DTO Projection cho các query danh sách phức tạp.

## 5. Security & Authentication
- **Lấy User hiện tại:** Sử dụng Bean `CurrentUserProvider.findCurrentUserOrThrow()` trong lớp Service, KHÔNG nhận `userId` từ body hay path variable nếu hành động đó do chính user thực hiện cho chính họ.
- **Token:** Hệ thống sử dụng cặp Access Token (JWT) và Refresh Token (lưu DB). Đảm bảo các luồng Rotate Refresh Token tuân thủ theo `AuthService`.

## 6. Testing (Kiểm thử)
- **Tên Method Test:** Theo chuẩn `[TênHàm]_[ĐiềuKiện]_[KếtQuảMongĐợi]`. (VD: `rejectFriendShip_NotAddressee_ThrowsException`).
- **Assertion:** Dùng `assertThrows` để bắt Exception và kiểm tra `ErrorCode` bên trong.
- Bất kỳ API mới nào được viết đều phải đi kèm với Integration Test mô phỏng luồng request thực tế bằng `MockMvc` và verify Response JSON structure.

## 7. Quy trình làm việc của AI (AI Workflow)
1. **Đọc Spec:** Mở và đọc `CHAT_API_SPEC_DETAILED.md` để nắm input/output.
2. **Nghiên cứu DB:** Kiểm tra các file Entity và Script `schema/` hiện có.
3. **Quản lý Dependency:** KHÔNG được phép thay đổi file `pom.xml` hoặc `build.gradle` (thêm/sửa thư viện ngoài) nếu chưa có sự đồng ý hoặc review từ Senior Engineer.
4. **Lên Plan:** Nếu logic phức tạp, dùng tool `enter_plan_mode` để thiết kế trước khi code.
5. **Triển khai:** Tuân thủ Clean Code, không hard-code (magic strings/numbers), tái sử dụng Service.
6. **Kiểm thử:** Chạy `mvnw clean test` để đảm bảo code mới không làm break hệ thống cũ. LUÔN LUÔN xử lý triệt để lỗi khi Unit Test fail.

## 8. Kiến trúc Phân lớp (Layered Architecture)
- Controller → Service → Repository. Không skip layer.
- Không inject Repository vào Controller.
- Không chứa business logic trong Controller.

## 9. DTO & Entity
- Không expose Entity ra ngoài Service layer.
- Input dùng *Request DTO + @Valid. Output dùng *Response DTO.

## 10. Anti-Patterns bị cấm
- System.out.println, e.printStackTrace()
- Hardcode magic string/number/secret
- Bắt Exception chung chung rồi bỏ qua
- Gọi nhiều query trong loop (N+1)