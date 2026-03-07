## Đặc tả lỗi API cho Frontend

Tài liệu này mô tả **format lỗi chuẩn** mà backend trả về, để FE có thể:

- Hiển thị thông báo thân thiện.
- Map `code` → hành vi (redirect, toast, focus field, v.v.).
- Log & debug dựa trên `traceId`.

---

## 1. Format JSON chung

Mọi response lỗi (4xx, 5xx) đều tuân theo cấu trúc:

```json
{
  "timestamp": "2025-03-07T10:30:00.123Z",
  "status": 400,
  "error": "Bad Request",
  "code": "VALIDATION_FAILED",
  "message": "Dữ liệu không hợp lệ",
  "path": "/api/auth/register",
  "traceId": "a1b2c3d4e5f6g7h8",
  "errors": [
    {
      "field": "username",
      "message": "Username must be between 3 and 50 characters",
      "rejectedValue": "ab"
    }
  ]
}
```

### 1.1. Ý nghĩa các field

- **timestamp**: Thời điểm server tạo lỗi (ISO-8601, UTC).
- **status**: HTTP status code (ví dụ `400`, `401`, `403`, `409`, `500`).
- **error**: HTTP status text tương ứng (`Bad Request`, `Unauthorized`, ...).
- **code**: **mã lỗi ổn định** cho FE (enum `ErrorCode`), dùng để `switch/case`.
- **message**: Thông điệp mô tả lỗi (tiếng Việt, hướng tới end-user).
- **path**: Endpoint gây ra lỗi (ví dụ `/api/auth/login`).
- **traceId**: ID request dùng để trace log backend. FE nên log lại khi có bug.
- **errors**: (optional) danh sách chi tiết lỗi – chủ yếu dùng cho **validation**.

### 1.2. Kiểu dữ liệu chi tiết lỗi (`errors`)

```json
{
  "field": "email",
  "message": "Email is not valid",
  "rejectedValue": "abc"
}
```

- **field**: Tên field trong payload (ví dụ: `username`, `email`, `password`).
- **message**: Thông báo lỗi cho field đó.
- **rejectedValue**: Giá trị FE gửi lên (có thể null để tránh lộ dữ liệu nhạy cảm).

---

## 2. Danh sách mã lỗi (`code`)

| Code                   | HTTP | Mô tả ngắn                                     | Khi nào xảy ra                                                |
|------------------------|------|-----------------------------------------------|----------------------------------------------------------------|
| `VALIDATION_FAILED`    | 400  | Dữ liệu không hợp lệ                          | Vi phạm `@Valid` trên DTO (register, login, ...)              |
| `INVALID_JSON`         | 400  | Body không phải JSON hợp lệ                   | JSON parse lỗi, thiếu/dư dấu ngoặc                             |
| `MISSING_PARAMETER`    | 400  | Thiếu tham số bắt buộc (query/path)          | Thiếu tham số request cần thiết                               |
| `UNAUTHORIZED`         | 401  | Chưa đăng nhập / token không hợp lệ          | Không có/ sai `Authorization: Bearer <token>`                 |
| `INVALID_CREDENTIALS`  | 401  | Sai username/password                         | Login thất bại                                                |
| `INVALID_REFRESH_TOKEN`| 401  | Refresh token không hợp lệ / hết hạn / thu hồi| Gọi `/api/auth/refresh` với token sai                         |
| `FORBIDDEN`            | 403  | Không có quyền truy cập                       | Thiếu quyền ở endpoint yêu cầu                               |
| `ACCOUNT_DISABLED`     | 403  | Tài khoản chưa được kích hoạt                | User `INACTIVE`                                               |
| `ACCOUNT_BANNED`       | 403  | Tài khoản bị cấm                              | User `BANNED`                                                 |
| `RESOURCE_NOT_FOUND`   | 404  | Không tìm thấy tài nguyên                     | (Dự phòng cho các API khác)                                  |
| `USERNAME_ALREADY_EXISTS` | 409 | Username đã tồn tại                         | Register với username đã dùng                                 |
| `EMAIL_ALREADY_EXISTS` | 409  | Email đã tồn tại                              | Register với email đã dùng                                    |
| `INTERNAL_ERROR`       | 500  | Lỗi hệ thống, không mong đợi                  | Exception không được handle cụ thể                            |

Lưu ý: **FE nên dựa vào `code` chứ không parse `message`** để quyết định hành vi.

---

## 3. Mapping code → hành vi gợi ý trên FE

| Code                         | Hành vi gợi ý trên FE                                                |
|------------------------------|----------------------------------------------------------------------|
| `UNAUTHORIZED`               | Redirect sang trang login, có thể hiển thị toast yêu cầu đăng nhập |
| `INVALID_CREDENTIALS`       | Hiển thị toast “Sai tài khoản hoặc mật khẩu”, không redirect       |
| `INVALID_REFRESH_TOKEN`     | Thử silent logout và redirect login                                 |
| `VALIDATION_FAILED`         | Map `errors[]` → lỗi cho từng input (username, email, ...)         |
| `USERNAME_ALREADY_EXISTS`   | Focus vào input username, hiển thị message cho field đó             |
| `EMAIL_ALREADY_EXISTS`      | Focus vào input email                                                |
| `ACCOUNT_BANNED`            | Hiển thị modal chặn thao tác, có thể hướng dẫn liên hệ hỗ trợ       |
| `ACCOUNT_DISABLED`          | Hiển thị hướng dẫn kích hoạt tài khoản                              |
| `FORBIDDEN`                 | Hiển thị trang “Không có quyền truy cập”                            |
| `INTERNAL_ERROR`            | Hiển thị thông báo chung và log `traceId` để gửi cho backend        |

---

## 4. TypeScript types gợi ý (FE)

```ts
export interface FieldErrorDetail {
  field: string;
  message: string;
  rejectedValue?: unknown;
}

export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  code: string;
  message: string;
  path: string;
  traceId?: string;
  errors?: FieldErrorDetail[];
}
```

FE có thể dùng type này cho mọi API call, không phụ thuộc vào từng endpoint.

---

## 5. Axios interceptor mẫu

```ts
import axios from 'axios';

axios.interceptors.response.use(
  (res) => res,
  (err) => {
    const data = err.response?.data as ApiErrorResponse | undefined;

    if (!data?.code) {
      // Fallback lỗi không chuẩn format (VD: network error)
      // showGenericErrorToast();
      return Promise.reject(err);
    }

    switch (data.code) {
      case 'INVALID_CREDENTIALS':
        // showToast(data.message);
        break;

      case 'VALIDATION_FAILED':
        // setFormErrors(data.errors ?? []);
        break;

      case 'INVALID_REFRESH_TOKEN':
      case 'UNAUTHORIZED':
        // handleAuthExpired(data);
        break;

      case 'USERNAME_ALREADY_EXISTS':
        // focusUsernameField();
        break;

      // ... các case khác nếu cần
      default:
        // showGenericErrorToast();
    }

    // Nên log traceId để debug khi có sự cố
    if (data.traceId) {
      console.debug('API error traceId:', data.traceId);
    }

    return Promise.reject(err);
  }
);
```

---

## 6. Gợi ý sử dụng `traceId`

- Backend gắn `X-Trace-Id` vào **tất cả** response.
- FE nên:
  - Đọc và log `traceId` khi có lỗi nghiêm trọng.
  - Cho phép user copy `traceId` khi gửi feedback/báo lỗi.
  - (Nâng cao) đính kèm `traceId` vào tool logging (Sentry, Datadog, ...).

Ví dụ hiển thị cho user:

> Đã xảy ra lỗi hệ thống. Mã theo dõi: `a1b2c3d4e5f6g7h8`. Vui lòng gửi mã này cho bộ phận hỗ trợ.

