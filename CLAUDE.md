# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```
./mvnw clean install         # Full build (skip tests: -DskipTests)
./mvnw spring-boot:run       # Run locally (needs PostgreSQL + RabbitMQ + Redis)
./mvnw verify -B             # CI-style build with tests
./mvnw test                  # Run all tests
./mvnw test -Dtest=ClassName # Single test class
./mvnw test -Dtest="*Service*,*Integration*" -DfailIfNoTests=false  # Pattern match
```

### Docker workflow
```
docker-compose up -d         # Start PostgreSQL, RabbitMQ, Redis
docker-compose build         # Build Docker image
```

## Project Architecture

**Spring Chat** — real-time messaging backend (REST + WebSocket), Java 21, Spring Boot 4.x.

### Layers (strictly enforced)
```
Controller (@RestController) → Service (interface + impl) → Repository (Spring Data JPA)
```
- No business logic in controllers. No Repository injection into controllers.
- Entities never exposed outside Service layer. All cross-layer data uses DTOs.

### Package layout
```
com.Spring_chat.Web_chat
├── config/          # DataInitializer, JacksonConfig, TraceIdFilter
├── controller/      # Auth, Conversation, Message, Friendship, Profile
├── dto/             # Request/Response DTOs (by domain sub-package)
├── entity/          # JPA entities with @Builder, @Getter/@Setter
├── enums/           # PermissionName, RoleName, MessageType, etc.
├── exception/       # AppException + ErrorCode enum + GlobalExceptionHandler
├── mappers/         # MapStruct interfaces (ConversationMapper, etc.)
├── repository/      # Spring Data JPA repositories
├── security/        # JWT auth, SecurityConfig, JwtAuthenticationFilter
└── service/         # Business logic (Auth, Conversation, Message, Friendship, User)
```

### Entities (6 main)
- **User** — core user with roles/permissions
- **Conversation** — PRIVATE or GROUP chat, with owner
- **ConversationParticipant** — join table with role, last-read timestamp, hidden status
- **Message** — text content with type, reply-to, client_message_id (idempotency)
- **MessageStatus** — per-participant delivery tracking (SENT/DELIVERED/SEEN)
- **Friendship** — directed requests with status (PENDING/ACCEPTED/REJECTED/BLOCKED)
- **RefreshToken** — JWT refresh tokens stored in DB
- **MessageHidden** — tracks hidden/deleted messages per user

### Key patterns

**API response format** — all success responses (except auth) wrapped in `ApiResponse<T>`:
```json
{"success": true, "message": "...", "data": {...}, "timestamp": "ISO-8601"}
```

**Errors** — throw `AppException(ErrorCode, message)`. Never throw raw RuntimeException. Error categories:
- `400 VALIDATION_FAILED` — invalid input
- `401 UNAUTHORIZED` — JWT issues
- `403 FORBIDDEN` — permission denied
- `404 RESOURCE_NOT_FOUND` — missing entity
- `422 BUSINESS_RULE_VIOLATED` — business logic violation

**Trace ID** — every request gets a correlation ID propagated via SLF4J MDC and `X-Trace-Id` response header.

**Current user** — use `CurrentUserProvider.findCurrentUserOrThrow()` in Service layer. Never accept userId from request body for self-actions.

**Pagination** — cursor-based for conversation list (`beforeId`), offset-based (Spring Pageable) for others.

**Database migrations** — SQL files in `src/main/resources/schema/` named `V<version>__<description>.sql`. Hibernate DDL is `validate` in production, `update` in dev.

**Testing** — H2 in-memory DB for unit tests; PostgreSQL service container in CI. Test method naming: `[MethodName]_[Condition]_[ExpectedResult]`. Integration tests use MockMvc.

## Tech Stack

- Java 21 + Virtual Threads (`spring.threads.virtual.enabled=true`)
- Spring Boot 4.x / Spring Framework 7.x
- PostgreSQL 15+, H2 (tests)
- Spring Security + JWT (jjwt 0.12.x) — stateless auth
- RabbitMQ — async mail/tasks
- Redis — OTP/password reset tokens
- MapStruct — DTO mapping
- Lombok — boilerplate reduction
- Cloudinary — avatar/file uploads
- Caffeine — local caching
- Jackson — JSON with `findAndRegisterModules()`

## API Specs

- `CHAT_API_SPEC_DETAILED.md` — full API contract (consult before adding/changing APIs)
- `API_ERROR_SPEC.md` — error code catalog
- `GEMINI.md` — detailed coding guidelines (AI workflow, conventions)
