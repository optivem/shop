# syntax=docker/dockerfile:1.4
# Build stage
FROM node:22-alpine AS build

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies
RUN --mount=type=cache,target=/root/.npm npm ci

# Copy source code
COPY . .

# Build the application
RUN npm run build

# Production stage — unprivileged variant runs as non-root nginx user (uid 101)
FROM nginxinc/nginx-unprivileged:alpine

# Copy built assets from build stage
COPY --from=build /app/dist /usr/share/nginx/html

# Copy nginx configuration template — image's entrypoint runs envsubst on /etc/nginx/templates/*.template
COPY nginx.conf /etc/nginx/templates/default.conf.template

# Limit envsubst to BACKEND_API_URL so nginx variables ($host, $remote_addr, ...) are left intact
ENV NGINX_ENVSUBST_FILTER=BACKEND_API_URL

# Expose port 3000 (unprivileged user can bind ports >1024)
EXPOSE 3000

HEALTHCHECK --interval=10s --timeout=3s --start-period=10s --retries=5 \
  CMD wget -qO- http://127.0.0.1:3000/ || exit 1

# Default CMD inherited from base image: ["nginx", "-g", "daemon off;"]
