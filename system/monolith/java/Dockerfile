# syntax=docker/dockerfile:1.4
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY src src
COPY config config
# Retry the wrapper's distribution download — a transient 5xx from the Gradle CDN
# otherwise kills the build (mirrors npm_config_fetch_retries in the TS images).
RUN --mount=type=cache,target=/root/.gradle \
    chmod +x gradlew && \
    for i in 1 2 3; do \
      ./gradlew --version --no-daemon && break; \
      [ "$i" = 3 ] && { echo "Gradle wrapper download failed after 3 attempts"; exit 1; }; \
      echo "Gradle wrapper download failed (attempt $i/3), retrying in 10s..."; sleep 10; \
    done && \
    ./gradlew bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build --chown=app:app /app/build/libs/*.jar app.jar
USER app
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=5 \
  CMD wget -qO- http://localhost:8080/livez || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
