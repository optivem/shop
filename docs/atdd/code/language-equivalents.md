# Language equivalents

`system/multitier/` carries the clean-architecture variant in Java today; `backend-clean-dotnet` and
`backend-clean-typescript` do not exist yet. Decisions taken in the Java implementation are recorded
here in language-neutral terms **before** the twins are written, so porting is one model into two
languages rather than three codebases changed after the fact.

A decision that only works in Java is not a decision. Each entry states the rule, then the shape it
takes in each language.

## Infrastructure failure signalling (gateways)

**Rule.** An external system failing to answer — a non-2xx it was not supposed to return, an IO
failure, a timeout, an interrupt — is its own failure class with its own exception type per gateway,
all under one base type, living in the infrastructure package next to the adapters that throw them.

It is **never** signalled with the language's built-in "programmer error" exception. That type stays
reserved for genuine bugs and misconfiguration, and the distinction is the whole point: a network
timeout and a bug in our own code must not arrive at the handler looking identical.

Catch clauses are narrowed to the checked/expected IO and parse failures. A blanket catch-all
re-wraps the adapter's own gateway exception in a second one and buries the original status.

| | Java | .NET | TypeScript |
|---|---|---|---|
| Base type | `abstract class GatewayException extends RuntimeException` | `abstract class GatewayException : Exception` | `abstract class GatewayException extends Error` |
| Per-gateway types | `TaxGatewayException`, `ClockGatewayException`, `ErpGatewayException` | same names | same names |
| Location | `infrastructure/external/` | `Infrastructure/External/` | `infrastructure/external/` |
| **Not** this | `IllegalStateException` | `InvalidOperationException` | bare `Error` / `TypeError` |
| Narrow the catch to | `IOException`, `InterruptedException` | `HttpRequestException`, `TaskCanceledException`, `JsonException` | `TypeError` from `fetch`, `SyntaxError` from `json()` |

**Misconfiguration is not a gateway failure.** An unknown external-system mode is a bad
configuration value, so it keeps the programmer-error type (`IllegalStateException` /
`InvalidOperationException` / `Error`). Do not fold it into the gateway family.

**HTTP status.** All gateway failures currently fall through to the catch-all 500. A dedicated
502/503 mapping is arguably more correct but is a behaviour change visible to the system tests; it is
deliberately deferred so it can be introduced and tested on its own.

## The catch-all 500 response body

**Rule.** An unhandled exception is by definition something we did not mean to expose. The full
message and every cause go to the log at ERROR; the response body carries a fixed string that says
nothing about the server's internals. Exception messages routinely name internal classes, SQL, and
host addresses.

| | Java | .NET | TypeScript |
|---|---|---|---|
| Handler | `@ExceptionHandler(Exception.class)` | `IExceptionHandler` / exception-handling middleware | error middleware |
| Body | `ProblemDetail` with a constant `detail` | `ProblemDetails` with a constant `Detail` | problem-details JSON with a constant `detail` |
| Log | `log.error("...", ex)` — the stack trace carries the causes | `ILogger.LogError(ex, "...")` | `logger.error(err)` |

The `type`, `title`, `status`, and `timestamp` fields are unchanged; only the free-text detail is
fixed.

## Unlimited quantities are null, not a sentinel

**Rule.** "No usage limit" is modelled as an absent value all the way through — request DTO, domain
value object, persisted column, response DTO. No `MAX_VALUE`-style sentinel is written to storage.

| | Java | .NET | TypeScript |
|---|---|---|---|
| Type | `Integer` | `int?` | `number \| null` |
| **Not** this | `Integer.MAX_VALUE` | `int.MaxValue` | `Number.MAX_SAFE_INTEGER` / `2147483647` |

The legacy services (`system/monolith/*`, `system/multitier/backend-{java,dotnet,typescript}`) still
write the sentinel — that is the before-picture and is left alone. All four front-ends already render
both `null` and `2147483647` as "Unlimited", so the clean variant can drop the sentinel without a
front-end change.
