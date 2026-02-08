#!/bin/sh
set -e

echo "Starting Nginx setup..."

# Load environment variables from .env.production if it exists
if [ -f /app/.env.production ]; then
    echo "Loading config from /app/.env.production"
    # Export các biến bắt đầu bằng VITE_ để dùng cho envsubst
    export $(grep -v '^#' /app/.env.production | xargs)
fi

# Set defaults if not present
export VITE_NGINX_LISTEN_PORT=${VITE_NGINX_LISTEN_PORT:-80}
export VITE_NGINX_SERVER_NAME=${VITE_NGINX_SERVER_NAME:-"localhost"}
export VITE_BACKEND_CONTAINER_NAME=${VITE_BACKEND_CONTAINER_NAME:-"student-backend"}
export VITE_BACKEND_CONTAINER_PORT=${VITE_BACKEND_CONTAINER_PORT:-"6868"}
export VITE_API_BASE_PATH=${VITE_API_BASE_PATH:-"/api/v1"}

echo "Generating nginx.conf from template..."
# Chỉ thay thế các biến cụ thể để tránh lỗi format nginx
envsubst '$VITE_NGINX_LISTEN_PORT $VITE_NGINX_SERVER_NAME $VITE_BACKEND_CONTAINER_NAME $VITE_BACKEND_CONTAINER_PORT $VITE_API_BASE_PATH' < /etc/nginx/templates/default.conf.template > /etc/nginx/conf.d/default.conf

echo "Configuration generated:"
cat /etc/nginx/conf.d/default.conf

echo "Starting Nginx..."
exec "$@"
