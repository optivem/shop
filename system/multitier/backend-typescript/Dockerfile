# syntax=docker/dockerfile:1.4

# Stage 1: Production-only deps (runs in parallel with the build stage)
FROM node:22-alpine@sha256:1314fb30ec37592d5845e5a160d522c0e680972aa534549940b8d73bde88481a AS deps-prod
WORKDIR /app
COPY package*.json ./
# npm retries transient registry failures (ECONNRESET, 5xx) on its own; raise
# the retry budget above the 2-retry default so a flaky network doesn't fail
# the build. Hard failures (404, ETARGET) still fail fast.
ENV npm_config_fetch_retries=5 \
    npm_config_fetch_retry_mintimeout=20000 \
    npm_config_fetch_retry_maxtimeout=120000
RUN --mount=type=cache,target=/root/.npm npm ci --omit=dev

# Stage 2: Build the app (full deps including devDependencies)
FROM node:22-alpine@sha256:1314fb30ec37592d5845e5a160d522c0e680972aa534549940b8d73bde88481a AS build
WORKDIR /app
COPY package*.json ./
# Raise npm's fetch-retry budget so transient registry blips don't fail the build.
ENV npm_config_fetch_retries=5 \
    npm_config_fetch_retry_mintimeout=20000 \
    npm_config_fetch_retry_maxtimeout=120000
RUN --mount=type=cache,target=/root/.npm npm ci
COPY . .
RUN npm run build

# Stage 3: Run the app
FROM node:22-alpine@sha256:1314fb30ec37592d5845e5a160d522c0e680972aa534549940b8d73bde88481a
WORKDIR /app
ENV NODE_ENV=production

# Bring in pre-built prod node_modules + compiled output
COPY --from=deps-prod /app/node_modules ./node_modules
COPY --from=build /app/dist ./dist
COPY --from=build /app/package*.json ./

# Drop privileges to the built-in node user
USER node

# Expose port
EXPOSE 8081

HEALTHCHECK --interval=10s --timeout=3s --start-period=15s --retries=5 \
  CMD wget -qO- http://localhost:8081/livez || exit 1

# Start the app
CMD ["node", "dist/main"]
