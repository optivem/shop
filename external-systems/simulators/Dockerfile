# syntax=docker/dockerfile:1.4
FROM node:25-alpine AS deps
WORKDIR /app
COPY package*.json ./
# npm retries transient registry failures (ECONNRESET, 5xx) on its own; raise
# the retry budget above the 2-retry default so a flaky network doesn't fail
# the build. Hard failures (404, ETARGET) still fail fast.
ENV npm_config_fetch_retries=5 \
    npm_config_fetch_retry_mintimeout=20000 \
    npm_config_fetch_retry_maxtimeout=120000
RUN --mount=type=cache,target=/root/.npm npm ci

FROM node:25-alpine
WORKDIR /app
ENV NODE_ENV=production
COPY --from=deps /app/node_modules ./node_modules
COPY mock-server.js package.json ./
USER node
EXPOSE 9000
HEALTHCHECK --interval=10s --timeout=3s --start-period=10s --retries=5 \
  CMD wget -qO- http://127.0.0.1:9000/erp/health || exit 1
CMD ["npm", "start"]
