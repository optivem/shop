# SLOs and alert rules

**$0 only.** The SLOs are measured and alerted on in the opt-in local `otel-lgtm` stack ([observability](../observability.md)); there is no paid alerting service, and the cloud stages have no alerting wired up.

## SLOs

Per backend service (`service` = `OTEL_SERVICE_NAME`, e.g. `shop-java-monolith`), over a 30-day window, health probes (`/livez`, `/readyz`, `/health`) left out:

| SLO | Target | Error budget | Good event |
|---|---|---|---|
| Availability | 99.5% | 0.5% | a server request whose span is not in error (no 5xx) |
| Latency | 95% | 5% | a server request that finishes within 512 ms |

512 ms rather than 500 ms because it is the span-metrics histogram bound closest to 500 ms.

## Where the numbers come from

The rules read Tempo's span metrics (`traces_spanmetrics_calls_total`, `traces_spanmetrics_latency_*`), which Tempo derives from the server spans every backend already exports. That makes one rule work for Java, .NET and TypeScript. The backends' own HTTP metrics are not usable for this: Java's `http_server_requests_milliseconds` has no latency buckets, .NET's `http_server_request_duration_seconds` has them in seconds, and the Next.js monolith emits no HTTP server metric at all.

Caveat: the TypeScript monolith (Next.js) emits two server spans per API request, an outer `GET /api/...` and a route span `GET /src/app/api/.../route`, and only the route span carries the error status. Its denominator counts each request twice, so its error ratio reads up to half the real one.

## Alert rules

[`docker/observability/grafana/alerting/shop-slo-alerts.yaml`](../../docker/observability/grafana/alerting/shop-slo-alerts.yaml), provisioned into Grafana (folder **SLOs**) by the `otel-lgtm` service in every compose file:

| Rule | Fires when | For | Severity |
|---|---|---|---|
| Availability fast burn | error ratio > 7.2% (14.4x budget) over both 1 h and 5 m | 2 m | page |
| Availability slow burn | error ratio > 3% (6x budget) over both 6 h and 30 m | 15 m | ticket |
| Latency burn | more than 10% of requests over 512 ms (2x budget) over both 1 h and 5 m | 5 m | ticket |

The long window keeps one bad minute from paging; the short one makes the alert stop soon after the problem does. No traffic means no data, which counts as OK.

There is no contact point: a firing alert shows in **Alerting → Alert rules** in Grafana (<http://localhost:3900>). Add a contact point in Grafana if you want notifications.

## Try it

1. Start a stack with the profile ([observability](../observability.md), Try it), e.g. the TypeScript monolith in stub mode.
2. Stop the ERP stub: `docker stop my-shop-stub-external-system-stubs-1`.
3. Call an API that needs ERP about a hundred times: `for i in $(seq 1 100); do curl -s -o /dev/null localhost:3312/api/products; done` (it answers `503`).
4. In Grafana, **Alerting → Alert rules → SLOs**: the fast burn rule goes Pending, then Firing after two minutes. Checked 2026-11-18 on the TypeScript monolith (the latency rule went Pending too: the failing calls wait for the ERP timeout).
5. Start the stub again (`docker start ...`); the rules return to Normal once the 5-minute window is clean.

See [dependency outage](dependency-outage.md) for what to do when one fires.
