# 🚀 Spring Chat - Enterprise Real-time Messaging Platform

[![Java Version](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4+-brightgreen?style=for-the-badge&logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15+-blue?style=for-the-badge&logo=postgresql)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Enabled-blue?style=for-the-badge&logo=docker)](https://www.docker.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge)](LICENSE)

---

## 🌏 English Version

### 📖 Introduction
**Spring Chat** is a high-performance, scalable real-time messaging engine built with **Java 21** and **Spring Boot 3**. Designed with a focus on security, concurrency, and modern architectural patterns, it leverages **Project Loom (Virtual Threads)** for massive scalability and a robust **RBAC + Permission** system for enterprise-grade access control.

### ✨ Key Features
- ⚡ **Real-time Performance**: Optimized for thousands of concurrent connections using Java 21 Virtual Threads.
- 🔐 **Advanced Security**: Stateless Authentication with JWT, Refresh Tokens, and a granular RBAC (Role-Based Access Control) + Permission system.
- 💬 **Rich Messaging**: Supports Private & Group chats, Message Status (Sent/Delivered/Seen), and Reply/Thread logic.
- 🔍 **Smart Search**: Integrated PostgreSQL Trigram indexing for lightning-fast user and message search.
- 🏗️ **Solid Architecture**: Clean code, Domain-driven Entity mapping, and automated Database Migrations.
- ⛓️ **CI/CD Ready**: Fully automated pipelines with GitHub Actions, Dockerized for seamless deployment.

### 🛠️ Tech Stack
- **Language**: Java 21 (LTS)
- **Framework**: Spring Boot 3.4+, Spring Security, Spring Data JPA
- **Database**: PostgreSQL 15+
- **Messaging/Queue**: RabbitMQ (for Async Mail/Tasks)
- **Documentation**: Swagger/OpenAPI (detailed specification)
- **DevOps**: Docker, Docker Compose, GitHub Actions (CI/CD)

---

## 🇻🇳 Phiên bản Tiếng Việt

### 📖 Giới thiệu
**Spring Chat** là một nền tảng backend cho hệ thống nhắn tin thời gian thực hiệu suất cao, được xây dựng trên nền tảng **Java 21** và **Spring Boot 3**. Dự án tập trung vào khả năng mở rộng, bảo mật tối đa và áp dụng các pattern kiến trúc hiện đại, sử dụng **Project Loom (Virtual Threads)** để xử lý hàng ngàn kết nối đồng thời và hệ thống phân quyền **RBAC + Permission** chi tiết.

### ✨ Tính năng nổi bật
- ⚡ **Hiệu suất vượt trội**: Tối ưu hóa cho hàng ngàn kết nối đồng thời nhờ Virtual Threads của Java 21.
- 🔐 **Bảo mật chuyên sâu**: Xác thực Stateless qua JWT, hỗ trợ Refresh Token, phân quyền đa lớp (Role & Permission).
- 💬 **Hệ thống tin nhắn**: Hỗ trợ chat cá nhân/nhóm, trạng thái tin nhắn (Đã gửi/Đã nhận/Đã xem) và trả lời theo luồng (Reply).
- 🔍 **Tìm kiếm thông minh**: Sử dụng PostgreSQL Trigram Index giúp tìm kiếm người dùng và tin nhắn cực nhanh.
- 🏗️ **Kiến trúc chuẩn mực**: Code sạch, mapping Entity chuẩn Domain-driven, quản lý DB Migration tự động.
- ⛓️ **Sẵn sàng triển khai**: Tích hợp sẵn CI/CD GitHub Actions, đóng gói Docker hoàn chỉnh.

### 🛠️ Công nghệ sử dụng
- **Ngôn ngữ**: Java 21 (LTS)
- **Framework**: Spring Boot 3.4+, Spring Security, Spring Data JPA
- **Cơ sở dữ liệu**: PostgreSQL 15+
- **Messaging**: RabbitMQ (xử lý Mail/Task bất đồng bộ)
- **Tài liệu API**: Swagger/OpenAPI
- **DevOps**: Docker, Docker Compose, GitHub Actions

---

## 🚀 Getting Started / Hướng dẫn bắt đầu

### 1. Prerequisites / Tiền đề
- JDK 21+
- Docker & Docker Compose
- Maven 3.9+

### 2. Configuration / Cấu hình
Create a `.env` file in the root directory (Tạo file `.env` tại thư mục gốc):
```env
# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/spring_chat
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=your_password

# JWT
JWT_SECRET=your_super_secret_key_at_least_256_bits
JWT_EXPIRATION=86400000
JWT_REFRESH_EXPIRATION=604800000

# Server
SERVER_PORT=8080
JPA_DDL_AUTO=update
```

### 3. Running with Docker / Chạy bằng Docker
```bash
docker-compose up -d
```

### 4. Running for Development / Chạy môi trường Dev
```bash
./mvnw clean install
./mvnw spring-boot:run
```

---

## 🏗️ System Architecture / Kiến trúc hệ thống

### 🔐 Security Flow (RBAC + Permission)
Hệ thống sử dụng mô hình **stateless** hoàn toàn:
1.  **Authentication**: User đăng nhập nhận cặp Access Token & Refresh Token.
2.  **Authorization**: JWT chứa danh sách `roles` và `permissions`.
3.  **Validation**: `JwtAuthenticationFilter` giải mã token và nạp quyền hạn vào `SecurityContext` mà không cần query lại database mỗi request.

### 🗄️ Database Architecture
Dự án bao gồm 6 thực thể chính (Entities) được tối ưu hóa cho hiệu suất cao và tính toàn vẹn dữ liệu. Để xem mô tả chi tiết từng bảng, sơ đồ ERD và các ràng buộc hệ thống, vui lòng truy cập:

👉 [**Tài liệu Database & Entity Mapping chi tiết**](./docs/DATABASE.md)

---

## 📝 Contribution & License
- **License**: MIT
- **Author**: [VUVANQUANIT](https://github.com/VUVANQUANIT)

*Cảm ơn bạn đã quan tâm đến Spring Chat! Nếu thấy hữu ích, hãy tặng chúng tôi một ⭐ nhé!*
