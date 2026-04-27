# syntax=docker/dockerfile:1.4
# Stage 1: Build the app
FROM node:22-alpine AS build
WORKDIR /app

# Install dependencies
COPY package*.json ./
RUN --mount=type=cache,target=/root/.npm npm ci

# Copy source code and build
COPY . .
RUN npm run build

# Stage 2: Run the app
FROM node:22-alpine
WORKDIR /app
ENV NODE_ENV=production

# Copy package files and install production dependencies
COPY --from=build /app/package*.json ./
RUN --mount=type=cache,target=/root/.npm npm ci --omit=dev

# Copy built files
COPY --from=build /app/dist ./dist

# Drop privileges to the built-in node user
USER node

# Expose port
EXPOSE 8081

HEALTHCHECK --interval=10s --timeout=3s --start-period=15s --retries=5 \
  CMD wget -qO- http://localhost:8081/health || exit 1

# Start the app
CMD ["node", "dist/main"]
