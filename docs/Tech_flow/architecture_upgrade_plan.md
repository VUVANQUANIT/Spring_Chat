# 🚀 Nâng cấp Spring Chat: Kafka, RabbitMQ, Redis & Microservices

> Dự án hiện tại: Spring Boot + WebSocket + JWT + PostgreSQL + Friendship/Conversation/Message system  
> Mục tiêu: Tích hợp các công nghệ hiện đại theo hướng học tập có hệ thống

---

## 📊 Tổng quan dự án hiện tại

```
Spring_Chat (Monolith)
├── Auth (JWT + Spring Security)
├── User management
├── Friendship system
├── Conversation (1-1 và Group)
├── Message
└── WebSocket (STOMP)
```

**Stack hiện tại:** Spring Boot 4.x, JPA/PostgreSQL, WebSocket STOMP, JWT, Lombok, MapStruct

---

## ✅ CÓ THỂ ÁP DỤNG ĐƯỢC - Phân tích từng công nghệ

---

## 1. 🔴 Redis — Nên làm TRƯỚC TIÊN (Dễ nhất, lợi ích rõ ràng nhất)

### Tại sao phù hợp?

Dự án chat có rất nhiều dữ liệu cần đọc nhanh, không cần query DB mỗi lần.

### Use Cases cụ thể trong dự án:


| Tính năng              | Hiện tại             | Sau khi có Redis                     |
| ---------------------- | -------------------- | ------------------------------------ |
| JWT Blacklist (logout) | Không có hoặc lưu DB | Lưu vào Redis với TTL = token expiry |
| Online/Offline status  | Không theo dõi       | `SET user:{id}:online 1 EX 30`       |
| Rate limiting chat     | Không có             | `INCR user:{id}:msg:count EX 60`     |
| Cache danh sách bạn bè | Query DB mỗi lần     | Cache 5 phút trong Redis             |
| Session WebSocket      | In-memory            | Redis pub/sub để scale               |


### Code ví dụ — Online Status Service:

```java
@Service
@RequiredArgsConstructor
public class UserPresenceService {
    private final RedisTemplate<String, String> redisTemplate;
    private static final String ONLINE_KEY = "user:online:";
    private static final long ONLINE_TTL = 30; // seconds

    public void markOnline(Long userId) {
        redisTemplate.opsForValue().set(
            ONLINE_KEY + userId, "1", Duration.ofSeconds(ONLINE_TTL)
        );
    }

    public boolean isOnline(Long userId) {
        return Boolean.TRUE.equals(
            redisTemplate.hasKey(ONLINE_KEY + userId)
        );
    }
}
```

### Code ví dụ — JWT Blacklist (sau logout):

```java
@Service
public class TokenBlacklistService {
    private final RedisTemplate<String, String> redisTemplate;

    public void blacklist(String token, long expiryMs) {
        redisTemplate.opsForValue().set(
            "blacklist:" + token, "1",
            Duration.ofMillis(expiryMs)
        );
    }

    public boolean isBlacklisted(String token) {
        return redisTemplate.hasKey("blacklist:" + token);
    }
}
```

### Thêm dependency:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

---

## 2. 🟠 RabbitMQ — Nên làm THỨ HAI (Message Queue cho Notification)

### Tại sao phù hợp?

Khi gửi tin nhắn, bạn cần notify cho người nhận. Nếu người nhận offline, cần queue lại để gửi sau (push notification, email, v.v.)

### Use Cases cụ thể:


| Luồng               | Không có RabbitMQ     | Có RabbitMQ                       |
| ------------------- | --------------------- | --------------------------------- |
| Gửi tin nhắn        | Send → Save DB → Done | Send → Save DB → Publish event    |
| Notify offline user | Mất notify            | Consumer lưu pending notification |
| Email notification  | Gọi thẳng (block)     | Async qua queue                   |
| Friend request      | Save DB → Done        | Save → Publish → Email async      |


### Architecture với RabbitMQ:

```
[User A gửi tin] 
      ↓
[MessageController] → Save to DB 
      ↓
[RabbitMQ: exchange=chat, routing=message.sent]
      ↓
[NotificationConsumer] → Check if B online?
      ├── Online → WebSocket push
      └── Offline → Save pending notification
```

### Code ví dụ:

```java
// Publisher
@Service
@RequiredArgsConstructor
public class MessageEventPublisher {
    private final RabbitTemplate rabbitTemplate;

    public void publishMessageSent(MessageSentEvent event) {
        rabbitTemplate.convertAndSend(
            "chat.exchange", 
            "message.sent", 
            event
        );
    }
}

// Consumer
@Component
@RequiredArgsConstructor
public class NotificationConsumer {
    private final UserPresenceService presenceService;
    private final SimpMessagingTemplate websocket;

    @RabbitListener(queues = "notification.queue")
    public void handleMessageSent(MessageSentEvent event) {
        Long recipientId = event.getRecipientId();
        if (presenceService.isOnline(recipientId)) {
            websocket.convertAndSendToUser(
                recipientId.toString(),
                "/queue/messages",
                event
            );
        } else {
            // Save pending notification to DB
        }
    }
}
```

### Thêm dependency:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

---

## 3. 🟡 Kafka — Nên làm THỨ BA (Event Streaming cho Analytics/Audit)

### Kafka vs RabbitMQ — Khi nào dùng cái nào?


|                | RabbitMQ                 | Kafka                      |
| -------------- | ------------------------ | -------------------------- |
| **Dùng cho**   | Task queue, notification | Event streaming, audit log |
| **Retention**  | Xóa sau khi consumed     | Lưu lâu dài (replay được)  |
| **Throughput** | 20k msg/s                | 1M+ msg/s                  |
| **Phù hợp**    | Notification, email      | Analytics, audit trail     |


### Use Cases trong dự án chat:


| Feature                  | Ứng dụng Kafka                                      |
| ------------------------ | --------------------------------------------------- |
| **Audit log**            | Mọi tin nhắn gửi đều stream vào topic `chat-events` |
| **Analytics**            | Đếm tin nhắn theo giờ, người dùng active nhất       |
| **Notification service** | Service riêng consume event và gửi push/email       |
| **Search indexing**      | Consumer index tin nhắn vào Elasticsearch           |


### Architecture:

```
[Message Service] → produce → [Kafka: topic=chat-messages]
                                       ↓
                         ┌─────────────────────────┐
                         │    Multiple Consumers    │
                         │                         │
                    [Analytics]  [Audit Log]  [Search Indexer]
```

### Code ví dụ:

```java
// Producer
@Service
@RequiredArgsConstructor
public class ChatEventProducer {
    private final KafkaTemplate<String, ChatMessageEvent> kafkaTemplate;

    public void sendMessageEvent(ChatMessageEvent event) {
        kafkaTemplate.send("chat-messages", 
            event.getConversationId().toString(), 
            event
        );
    }
}

// Consumer
@Service
public class ChatAuditConsumer {
    @KafkaListener(topics = "chat-messages", groupId = "audit-service")
    public void auditMessage(ChatMessageEvent event) {
        // Lưu vào audit log hoặc Elasticsearch
        log.info("Message audit: conversationId={}, sender={}", 
            event.getConversationId(), event.getSenderId());
    }
}
```

### Thêm dependency:

```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

---

## 4. 🟣 Microservices — Nên làm CUỐI CÙNG (Sau khi thành thạo các tech trên)

### Tách dự án thành các service:

```
                    [API Gateway (Spring Cloud Gateway)]
                              ↓
           ┌──────────────────────────────────────┐
           │          Service Registry             │
           │         (Eureka / Consul)             │
           └──────────────────────────────────────┘
                ↑         ↑         ↑         ↑
    [Auth     ] [User   ] [Chat   ] [Notify  ]
    [Service  ] [Service] [Service] [Service ]
         ↓          ↓        ↓          ↓
      [JWT DB]  [Users DB] [Msgs DB] [Redis/RMQ]
```

### Cách tách từ monolith hiện tại:


| Service                  | Từ code hiện tại                           | Tech                            |
| ------------------------ | ------------------------------------------ | ------------------------------- |
| **auth-service**         | `AuthController` + JWT logic               | Spring Boot + Redis (blacklist) |
| **user-service**         | `UserController` + User entity             | Spring Boot + PostgreSQL        |
| **friendship-service**   | `FriendShipController` + Friendship entity | Spring Boot + PostgreSQL        |
| **chat-service**         | Conversation + Message + WebSocket         | Spring Boot + WebSocket + Kafka |
| **notification-service** | Mới hoàn toàn                              | Spring Boot + RabbitMQ          |
| **api-gateway**          | Mới                                        | Spring Cloud Gateway            |


### Giao tiếp giữa services:

```
Synchronous (REST/gRPC):
  user-service → friendship-service: "Get user info"

Asynchronous (Kafka/RabbitMQ):
  chat-service → (Kafka) → notification-service: "Message sent"
```

---

## 📅 Lộ trình học tập đề xuất

```
Tuần 1-2:   Redis
  └── JWT blacklist, online status, friendship cache

Tuần 3-4:   RabbitMQ  
  └── Notification queue khi gửi tin nhắn, friend request event

Tuần 5-6:   Kafka
  └── Audit log, analytics pipeline

Tuần 7-10:  Microservices
  └── Tách auth + user + chat service
  └── API Gateway (Spring Cloud Gateway)
  └── Service Discovery (Eureka)
  └── Config Server
```

---

## 🐳 Docker Compose để chạy tất cả

```yaml
# Thêm vào docker-compose.yml hiện tại
version: '3.8'
services:
  
  # Redis
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  # RabbitMQ
  rabbitmq:
    image: rabbitmq:3-management
    ports:
      - "5672:5672"
      - "15672:15672"  # Management UI
    environment:
      RABBITMQ_DEFAULT_USER: admin
      RABBITMQ_DEFAULT_PASS: admin

  # Kafka + Zookeeper
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
```

---

## 🎯 Kết luận


| Công nghệ         | Độ phù hợp | Độ khó        | Ưu tiên      |
| ----------------- | ---------- | ------------- | ------------ |
| **Redis**         | ⭐⭐⭐⭐⭐      | Dễ            | **Làm ngay** |
| **RabbitMQ**      | ⭐⭐⭐⭐       | Trung bình    | Sau Redis    |
| **Kafka**         | ⭐⭐⭐        | Khó hơn       | Sau RabbitMQ |
| **Microservices** | ⭐⭐⭐⭐       | Phức tạp nhất | Cuối cùng    |


> 💡 **Gợi ý**: Bắt đầu với Redis vì lợi ích rõ ràng ngay (online status + JWT blacklist), code đơn giản, và là nền tảng cho các tech sau.

