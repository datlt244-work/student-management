# Stage 1: Build
FROM node:20-alpine as build-stage
WORKDIR /app

# Install pnpm
RUN npm install -g pnpm

# Copy package files
COPY package.json pnpm-lock.yaml ./

# Install dependencies
RUN pnpm install --frozen-lockfile

# Copy source code (bao gồm .env.development và .env.production nếu có)
COPY . .

# Xử lý environment files cho Vite build
# Vite tự động load .env.production khi build với mode production
# Nếu có .env.development, copy thành .env.production để Vite load
# Nếu đã có .env.production, giữ nguyên
RUN echo "Checking for environment files..." && \
    ls -la .env* 2>/dev/null || echo "No .env files found" && \
    if [ -f .env.development ] && [ ! -f .env.production ]; then \
      cp .env.development .env.production && \
      echo "✓ Copied .env.development to .env.production"; \
    elif [ -f .env.production ]; then \
      echo "✓ Using existing .env.production"; \
    else \
      echo "⚠ No .env file found, building without environment variables"; \
    fi && \
    echo "Environment files after processing:" && \
    ls -la .env* 2>/dev/null || echo "No .env files"

# Build the application (Vite sẽ tự động load .env.production nếu có)
RUN pnpm build

# Stage 2: Production Run
FROM nginx:stable-alpine as production-stage

# Install gettext for envsubst command
RUN apk add --no-cache gettext

# Copy built files from build stage
COPY --from=build-stage /app/dist /usr/share/nginx/html

# Copy nginx template and .env.production
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build-stage /app/.env.production /tmp/.env.production

# Create entrypoint script to generate nginx.conf from template using .env.production
RUN echo '#!/bin/sh' > /docker-entrypoint.sh && \
    echo 'set -a' >> /docker-entrypoint.sh && \
    echo '. /tmp/.env.production' >> /docker-entrypoint.sh && \
    echo 'envsubst "$$VITE_NGINX_LISTEN_PORT $$VITE_NGINX_SERVER_NAME $$VITE_API_BASE_PATH $$VITE_BACKEND_CONTAINER_NAME $$VITE_BACKEND_CONTAINER_PORT" < /etc/nginx/templates/default.conf.template > /etc/nginx/conf.d/default.conf' >> /docker-entrypoint.sh && \
    echo 'rm /tmp/.env.production' >> /docker-entrypoint.sh && \
    echo 'exec nginx -g "daemon off;"' >> /docker-entrypoint.sh && \
    chmod +x /docker-entrypoint.sh

# Expose port 80
EXPOSE 80

# Use custom entrypoint
ENTRYPOINT ["/docker-entrypoint.sh"]