# ============================================
# Stage 1: Build
# ============================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy các file cấu hình Maven từ thư mục hiện tại (backend/)
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Cho phép chạy file mvnw
RUN chmod +x mvnw

# Download dependencies (cached layer)
RUN ./mvnw dependency:go-offline -B

# Copy mã nguồn
COPY src src

# Build JAR
RUN ./mvnw package -DskipTests -B

# ============================================
# Stage 2: Runtime
# ============================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Cài đặt wget cho healthcheck
RUN apk add --no-cache wget

# Bảo mật: chạy với user không có quyền root
RUN addgroup -g 1000 appgroup && adduser -u 1000 -G appgroup -D appuser

# Copy file JAR đã build từ stage builder
COPY --from=builder /app/target/*.jar app.jar

USER appuser

EXPOSE 6868

# Health check (Actuator)
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:6868/api/v1/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]

ENV SPRING_PROFILES_ACTIVE=prod