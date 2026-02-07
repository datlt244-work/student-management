# Stage 1: Build
FROM node:20-alpine as build-stage
WORKDIR /app

# 1. Cố định phiên bản pnpm giống máy cá nhân của bạn để tránh lỗi Lockfile
RUN npm install -g pnpm@10.4.1

# Copy package files
COPY package.json pnpm-lock.yaml ./

# 2. Thay --frozen-lockfile bằng --no-frozen-lockfile nếu vẫn gặp lỗi không khớp
# Nhưng vì đã cố định pnpm@10.4.1 ở trên, bạn có thể dùng --frozen-lockfile để an toàn
RUN pnpm install --no-frozen-lockfile

# Copy source code
COPY . .

# Xử lý environment files
RUN if [ -f .env.development ] && [ ! -f .env.production ]; then \
      cp .env.development .env.production; \
    fi

# Build (Vite sẽ đóng gói các biến VITE_* vào file tĩnh)
RUN pnpm build

# Stage 2: Production Run
FROM nginx:stable-alpine as production-stage

RUN apk add --no-cache gettext

# Copy built files
COPY --from=build-stage /app/dist /usr/share/nginx/html

# Copy nginx template và file env để envsubst (dùng cho config Nginx động)
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build-stage /app/.env.production /tmp/.env.production

# Entrypoint script
RUN echo '#!/bin/sh' > /docker-entrypoint.sh && \
    echo 'set -a' >> /docker-entrypoint.sh && \
    echo '[ -f /tmp/.env.production ] && . /tmp/.env.production' >> /docker-entrypoint.sh && \
    echo 'envsubst "$$VITE_NGINX_LISTEN_PORT $$VITE_NGINX_SERVER_NAME $$VITE_API_BASE_PATH $$VITE_BACKEND_CONTAINER_NAME $$VITE_BACKEND_CONTAINER_PORT" < /etc/nginx/templates/default.conf.template > /etc/nginx/conf.d/default.conf' >> /docker-entrypoint.sh && \
    echo 'exec nginx -g "daemon off;"' >> /docker-entrypoint.sh && \
    chmod +x /docker-entrypoint.sh

EXPOSE 80

ENTRYPOINT ["/docker-entrypoint.sh"]
