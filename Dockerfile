# =============================================================================
# Stage 1: Build — compile và package JAR
# =============================================================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper và pom.xml trước để cache dependencies layer
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

RUN chmod +x mvnw

# Download dependencies (cache layer riêng — chỉ rebuild khi pom.xml thay đổi)
RUN ./mvnw dependency:go-offline -B --no-transfer-progress

# Copy source code và build
COPY src ./src
RUN ./mvnw package -DskipTests -B --no-transfer-progress

# =============================================================================
# Stage 2: Runtime — image nhỏ gọn, không chứa JDK hay source
# =============================================================================
FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

# Tạo non-root user để chạy app (bảo mật)
RUN addgroup -S spring && adduser -S spring -G spring

# Copy JAR từ build stage
COPY --from=builder /app/target/*.jar app.jar

# Đổi owner file sang user spring
RUN chown spring:spring app.jar

USER spring:spring

EXPOSE 8080

# JVM flags tối ưu cho container:
# -XX:+UseContainerSupport       : Đọc RAM/CPU từ container limits (không phải host)
# -XX:MaxRAMPercentage=75.0      : Dùng tối đa 75% RAM container
# -XX:+UseZGC                    : ZGC garbage collector — tốt cho low-latency (Java 21)
# -Djava.security.egd=...        : Tăng tốc khởi động (entropy source)
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+UseZGC", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
