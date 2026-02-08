# Stage 1: Build
FROM node:20-alpine as build-stage
WORKDIR /app

# Cài đặt pnpm
RUN npm install -g pnpm@10.4.1

# Copy package files
COPY package.json pnpm-lock.yaml ./

# Cài đặt dependencies (Dùng frozen-lockfile để đảm bảo tính nhất quán với máy dev)
RUN pnpm install --frozen-lockfile

# Copy source code và .env.production (được tạo từ GitHub Actions)
COPY . .

# Kiểm tra file env trước khi build (để debug)
RUN if [ -f .env.production ]; then \
      echo "✓ Found .env.production"; \
    else \
      echo "❌ Error: .env.production not found!"; exit 1; \
    fi

# Build Vite (Vite sẽ bake các biến VITE_* vào file tĩnh HTML/JS)
RUN pnpm build

# Stage 2: Production Run
FROM nginx:stable-alpine as production-stage

# Cài đặt gettext để có lệnh envsubst
RUN apk add --no-cache gettext

# Copy static files từ build-stage
COPY --from=build-stage /app/dist /usr/share/nginx/html

# Copy nginx template
COPY nginx.conf.template /etc/nginx/templates/default.conf.template

# Copy file .env.production để lấy tham số cấu hình Nginx lúc chạy
COPY --from=build-stage /app/.env.production /app/.env.production

# Tạo entrypoint script
# Script này sẽ đọc .env.production, export biến, và chạy envsubst để tạo nginx.conf
COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh

ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["nginx", "-g", "daemon off;"]
