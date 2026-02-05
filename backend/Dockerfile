# ============================================
# Stage 1: Build
# ============================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Download dependencies (cached layer)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src src

# Build JAR (skip tests for faster builds in production)
RUN ./mvnw package -DskipTests -B

# ============================================
# Stage 2: Runtime
# ============================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Install wget for healthcheck
RUN apk add --no-cache wget

# Create non-root user for security
RUN addgroup -g 1000 appgroup && adduser -u 1000 -G appgroup -D appuser

# Copy JAR from builder
COPY --from=builder /app/target/*.jar app.jar

# Switch to non-root user
USER appuser

# Expose application port
EXPOSE 6868

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:6868/api/v1/actuator/health || exit 1

# Run application with prod profile
ENTRYPOINT ["java", "-jar", "app.jar"]

# Default: use prod profile
ENV SPRING_PROFILES_ACTIVE=prod
