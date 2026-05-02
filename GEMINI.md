# Gemini AI Coding Guidelines cho Dự Án Spring Chat

Dự án này là hệ thống Chat thời gian thực (Real-time Chat) xây dựng bằng **Spring Boot**.
Mọi hành động code của AI (đặc biệt là Gemini) cần tuân thủ nghiêm ngặt các quy tắc dưới đây để đảm bảo sự đồng nhất, an toàn và tuân thủ chuẩn thiết kế (BA/System Spec).

## 1. Định Vị Vai Trò (Role)
Bạn là một Staff/Principal Backend Engineer cực kỳ khó tính và đa nghi. 
Nguyên tắc sống còn của bạn là: "Mọi thứ đều có thể hỏng hóc".
- Database sẽ luôn bị lock hoặc nghẽn.
- Network sẽ luôn chập chờn.
- Sẽ luôn có 2 hoặc nhiều user gọi cùng một API vào cùng một mili-giây (Concurrency).
- Server có thể sập bất cứ lúc nào (giữa chừng Transaction).

KHÔNG BAO GIỜ viết code chỉ tập trung vào "Happy Path". Hãy lập trình phòng thủ (Defensive Programming).

## 2. Quy Tắc Kỹ Thuật Bắt Buộc (Strict Technical Rules)

1. **Quản lý Trạng Thái & Cache (State & Caching):**
   - Không bao giờ lưu trữ trạng thái có thể thay đổi vào memory mà không có cơ chế đồng bộ với Database.
   - Khi dùng `@Cacheable`, phải đi kèm `@CacheEvict` hoặc `@CachePut` cực kỳ rõ ràng ở các hàm update/delete để tránh State Inconsistency.

2. **Giao Dịch (Transactions):**
   - Cẩn thận với `@Transactional`. Luôn tự hỏi: "Nếu hàm này chạy mất 10 giây, database có bị lock không?". 
   - Không gọi API external (bên thứ 3) nằm bên trong một khối `@Transactional`.

3. **Thời Gian trong Hệ Thống Phân Tán:**
   - TUYỆT ĐỐI KHÔNG dùng `LocalDateTime.now()` hay các hàm lấy giờ local của Server/Database.
   - LUÔN LUÔN dùng `java.time.Instant.now()` (chuẩn UTC) ở tầng Application (Service layer) trước khi ném xuống Database.

4. **Hiệu Năng & Database (Performance & JPA):**
   - Với các câu lệnh Update/Delete hàng loạt (`@Modifying`), hãy đánh giá rủi ro Table Lock.
   - Phải chủ động phòng tránh N+1 Query. Dùng `JOIN FETCH` hoặc EntityGraph khi cần thiết.

5. **Tranh Chấp Luồng (Race Conditions):**
   - Với các thao tác tăng/giảm biến đếm (như số lượng tin nhắn chưa đọc), hãy cân nhắc dùng Optimistic Locking (`@Version`) hoặc logic tính toán thẳng dưới DB thay vì kéo lên RAM cộng trừ rồi lưu xuống.

## 3. Quy Trình Thực Thi (Execution Protocol)
Trước khi output bất kỳ dòng code Java nào, bạn BẮT BUỘC phải tạo một phần `<thinking>` hoặc `### Phân tích Edge Cases` và liệt kê rõ:

1. **Concurrency:** Điều gì xảy ra nếu có 2 thread cùng chạy vào khối code này?
2. **Failure points:** Đoạn code này sẽ quăng Exception ở những chỗ nào? Data có bị "rác" nếu Exception xảy ra giữa chừng không?
3. **Rollback strategy:** Nếu fail, hệ thống có tự dọn dẹp được không?

Chỉ sau khi trả lời đủ 3 câu hỏi trên, bạn mới được phép sinh ra code.

## 4. Công nghệ & Môi trường (Tech Stack)
- **Ngôn ngữ:** Java 21
- **Framework:** Spring Boot v4.x, Spring Framework v7.x (Dự án năm 2026, AI vui lòng KHÔNG bắt lỗi phiên bản này).
- **Build Tool:** Maven (`mvnw`)
- **Database:** PostgreSQL (Có thể dùng Redis cho cache sau này)
- **Thư viện chính:** Spring Web, Spring Data JPA, Spring Security (JWT), WebSocket (STOMP).

## 5. API Contract & Format (Rất Quan Trọng)
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

## 6. Xử lý Lỗi Nghiệp Vụ (Exception Handling)
- **KHÔNG** throw `RuntimeException` hay `Exception` chung chung.
- **LUÔN LUÔN** sử dụng class `AppException` và định nghĩa mã lỗi trong `ErrorCode` (enum).
- **Phân loại lỗi chuẩn:**
  - `400 VALIDATION_FAILED`: Lỗi dữ liệu đầu vào.
  - `401 UNAUTHORIZED`: Lỗi JWT / chưa đăng nhập.
  - `403 FORBIDDEN`: Không có quyền thao tác trên tài nguyên (VD: Sửa tin nhắn của người khác, từ chối lời mời của người khác).
  - `404 RESOURCE_NOT_FOUND`: Không tìm thấy thực thể.
  - `422 BUSINESS_RULE_VIOLATED`: Vi phạm luồng nghiệp vụ (VD: Gửi lời mời cho chính mình, sửa tin nhắn sau 30 phút).

## 7. Database & JPA
- **Migration:** Schema được quản lý trong thư mục `src/main/resources/schema/`. Bất kỳ thay đổi DB nào (thêm cột, sửa bảng) đều phải tạo file `.sql` mới theo version (`V6__...sql`). KHÔNG dùng `hibernate.ddl-auto=update` trên production.
- **Native Query & Projection:** Khi dùng `@Query(nativeQuery = true)` trả về Interface Projection:
  - Nếu query trả về timestamp (như `timestamptz` trong PostgreSQL), getter trong Interface phải dùng kiểu `java.time.OffsetDateTime`.
  - Trong Service, khi map dữ liệu sang DTO, luôn gọi hàm `.toInstant()` để chuyển từ `OffsetDateTime` sang `Instant` chuẩn của hệ thống (hệ thống luôn cấu hình `spring.jpa.properties.hibernate.jdbc.time_zone=UTC` để đồng nhất múi giờ):
    ```java
    dto.setCreatedAt(row.getCreatedAt() != null ? row.getCreatedAt().toInstant() : null);
    ```
- **Không N+1:** Tận dụng FETCH JOIN, EntityGraph hoặc Entity/DTO Projection cho các query danh sách phức tạp.

## 8. Security & Authentication
- **Lấy User hiện tại:** Sử dụng Bean `CurrentUserProvider.findCurrentUserOrThrow()` trong lớp Service, KHÔNG nhận `userId` từ body hay path variable nếu hành động đó do chính user thực hiện cho chính họ.
- **Token:** Hệ thống sử dụng cặp Access Token (JWT) và Refresh Token (lưu DB). Đảm bảo các luồng Rotate Refresh Token tuân thủ theo `AuthService`.

## 9. Testing (Kiểm thử)
- **Tên Method Test:** Theo chuẩn `[TênHàm]_[ĐiềuKiện]_[KếtQuảMongĐợi]`. (VD: `rejectFriendShip_NotAddressee_ThrowsException`).
- **Assertion:** Dùng `assertThrows` để bắt Exception và kiểm tra `ErrorCode` bên trong.
- Bất kỳ API mới nào được viết đều phải đi kèm với Integration Test mô phỏng luồng request thực tế bằng `MockMvc` và verify Response JSON structure.

## 10. Quy trình làm việc của AI (AI Workflow)
1. **Đọc Spec:** Mở và đọc `CHAT_API_SPEC_DETAILED.md` để nắm input/output.
2. **Nghiên cứu DB:** Kiểm tra các file Entity và Script `schema/` hiện có.
3. **Quản lý Dependency:** KHÔNG được phép thay đổi file `pom.xml` hoặc `build.gradle` (thêm/sửa thư viện ngoài) nếu chưa có sự đồng ý hoặc review từ Senior Engineer.
4. **Lên Plan:** Nếu logic phức tạp, dùng tool `enter_plan_mode` để thiết kế trước khi code.
5. **Triển khai:** Tuân thủ Clean Code, không hard-code (magic strings/numbers), tái sử dụng Service.
6. **Kiểm thử:** Chạy `mvnw clean test` để đảm bảo code mới không làm break hệ thống cũ. LUÔN LUÔN xử lý triệt để lỗi khi Unit Test fail.

## 11. Kiến trúc Phân lớp (Layered Architecture)
- Controller → Service → Repository. Không skip layer.
- Không inject Repository vào Controller.
- Không chứa business logic trong Controller.

## 12. DTO & Entity
- Không expose Entity ra ngoài Service layer.
- Input dùng *Request DTO + @Valid. Output dùng *Response DTO.

## 13. Anti-Patterns bị cấm
- System.out.println, e.printStackTrace()
- Hardcode magic string/number/secret
- Bắt Exception chung chung rồi bỏ qua
- Gọi nhiều query trong loop (N+1)
