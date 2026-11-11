# Observability

Opt-in and $0: one [`grafana/otel-lgtm`](https://github.com/grafana/docker-otel-lgtm) container (OpenTelemetry Collector + Tempo traces + Loki logs + Prometheus metrics + Grafana) sits behind the `observability` Docker Compose profile in every file under [docker/](../docker/). With the profile off, nothing changes: the `otel-lgtm` service is not started and `OTEL_EXPORTER_OTLP_ENDPOINT` is not set in the backend container, so the backends export nothing.

## How it is wired

- **Compose:** the `otel-lgtm` service ([example](../docker/java/monolith/docker-compose.local.real.yml)) is byte-identical across all 24 compose files; [compose-drift.yml](../.github/workflows/compose-drift.yml) fails the build if a copy drifts.
- **Backends** (`system` in monolith, `backend` in multitier) get a fixed `OTEL_SERVICE_NAME` (e.g. `shop-java-monolith`, `shop-dotnet-multitier-backend`), `OTEL_EXPORTER_OTLP_PROTOCOL=http/protobuf`, and `OTEL_EXPORTER_OTLP_ENDPOINT` passed through from your shell. They export OTLP only when that endpoint is set and non-empty, and log JSON with `trace_id` / `span_id`.
- **Frontend (multitier):** the React app starts browser tracing only when its container gets `OTEL_BROWSER_EXPORTER_OTLP_ENDPOINT` (passed through from your shell, like the backend variable). Set it to the host URL `http://localhost:4318`, since the browser runs outside the Docker network. The browser's `fetch` calls then carry `traceparent`, so the trace starts in the browser. The collector in `otel-lgtm` already accepts CORS from `http://*` origins.
- **Host ports:** Grafana `3900`, OTLP gRPC `4317`, OTLP HTTP `4318`. The ports are fixed, so run the profile on one stack at a time.

## Try it

1. Start one stack with the profile on, from the repo root (Java monolith, real mode shown; any file under `docker/` works the same way):

   ```bash
   OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-lgtm:4318 docker compose -f docker/java/monolith/docker-compose.local.real.yml --profile observability up -d --build
   ```

   PowerShell:

   ```pwsh
   $env:OTEL_EXPORTER_OTLP_ENDPOINT = "http://otel-lgtm:4318"; docker compose -f docker/java/monolith/docker-compose.local.real.yml --profile observability up -d --build
   ```

   For a multitier stack, also set `OTEL_BROWSER_EXPORTER_OTLP_ENDPOINT=http://localhost:4318` to include browser spans.

2. Place an order in the shop UI (for this stack, <http://localhost:3111>).
3. Open Grafana at <http://localhost:3900>. In **Explore → Tempo**, search by service name `shop-java-monolith` and open the trace of the place-order `POST` (route names differ slightly per language). The trace shows the inbound request with the outbound ERP / Clock / Tax HTTP calls as child spans (the TypeScript backends also trace Postgres queries).
4. Correlated logs: from a span, use **Logs for this span**, or in **Explore → Loki** run `{service_name="shop-java-monolith"} | trace_id="<trace id>"`. Every log line written while handling the request carries the same `trace_id`.
5. Stop it with the same `-f <file> --profile observability down`.
