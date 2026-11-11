package com.mycompany.myshop.backend.infrastructure.external;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.core.functions.CheckedSupplier;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.java11.instrument.binder.jdk.MicrometerHttpClient;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

// The one place that knows how to call a system we do not control: a tight connect/read timeout, a
// retry with exponential backoff and jitter, and a circuit breaker (Resilience4j), one instance per
// external system.
//
// GET only, and that is what makes the retry safe: repeating a read cannot duplicate a side effect.
// If a POST ever needs to go through here, it must not be retried.
//
// Only transient failures are retried and counted by the breaker: a timeout, a refused or dropped
// connection, a 5xx. A 4xx -- the ERP's 404 "no such product" included -- is an answer about our
// request; repeating it cannot change it, and it says nothing about the other system's health.
//
// It deliberately does not interpret the response. Status meaning is the caller's: a 5xx that is
// still failing after the last attempt is returned as a response, and an open breaker surfaces as an
// IOException, so each gateway keeps building its own exception, naming the SKU or the country it
// was asking about, from the same two paths it already handles. No fallback value is ever invented
// here: a guessed price is a money bug, so the caller is told the system did not answer.
public final class GatewayHttpClient {

    private static final Logger log = LoggerFactory.getLogger(GatewayHttpClient.class);

    private final String name;
    private final Duration readTimeout;
    private final Retry retry;
    private final CircuitBreaker circuitBreaker;

    // One per external system rather than per call: an HttpClient owns a connection pool and an
    // executor, and the breaker only means something if every call to that system goes through it.
    private final HttpClient httpClient;

    public GatewayHttpClient(String name, ResilienceSettings settings) {
        this.name = name;
        this.readTimeout = settings.readTimeout();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(settings.connectTimeout())
                .build();

        var retrySettings = settings.retry();
        this.retry = Retry.of(name, RetryConfig.custom()
                .maxAttempts(retrySettings.maxAttempts())
                .intervalFunction(IntervalFunction.ofExponentialRandomBackoff(
                        retrySettings.initialBackoff(),
                        retrySettings.backoffMultiplier(),
                        retrySettings.jitter()))
                .retryOnException(GatewayHttpClient::isTransient)
                .build());
        this.retry.getEventPublisher().onRetry(event -> log.warn(
                "Retrying {} call, attempt {} of {} failed with {}", name,
                event.getNumberOfRetryAttempts(), retrySettings.maxAttempts(), describe(event.getLastThrowable())));

        var breakerSettings = settings.circuitBreaker();
        this.circuitBreaker = CircuitBreaker.of(name, CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(breakerSettings.slidingWindowSize())
                .minimumNumberOfCalls(breakerSettings.minimumNumberOfCalls())
                .failureRateThreshold(breakerSettings.failureRateThreshold())
                .waitDurationInOpenState(breakerSettings.waitInOpenState())
                .permittedNumberOfCallsInHalfOpenState(breakerSettings.permittedCallsInHalfOpenState())
                .recordException(GatewayHttpClient::isTransient)
                .build());
        this.circuitBreaker.getEventPublisher().onStateTransition(event -> log.warn(
                "Circuit breaker for {} moved {}", name, event.getStateTransition()));
    }

    // The registry gives every attempt its own client span and a W3C traceparent header. Wrapping is
    // per call and cheap: the instrumented client delegates to the pooled one.
    public HttpResponse<String> get(String url, ObservationRegistry observationRegistry)
            throws IOException, InterruptedException {
        var client = MicrometerHttpClient.instrumentationBuilder(httpClient, Metrics.globalRegistry)
                .observationRegistry(observationRegistry)
                .build();
        var request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(readTimeout)
                .GET()
                .build();

        CheckedSupplier<HttpResponse<String>> attempt = () -> {
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 500) {
                throw new ServerErrorResponse(response);
            }
            return response;
        };

        // Retry outside the breaker: every attempt is one call the breaker counts, and once it opens
        // the retry stops at once instead of waiting out its backoff against a known outage.
        var guarded = Retry.decorateCheckedSupplier(retry,
                CircuitBreaker.decorateCheckedSupplier(circuitBreaker, attempt));

        try {
            return guarded.get();
        } catch (ServerErrorResponse e) {
            return e.response;
        } catch (CallNotPermittedException e) {
            throw new IOException("Circuit breaker for " + name + " is open; not calling " + url, e);
        } catch (IOException | InterruptedException | RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new IOException(e);
        }
    }

    private static boolean isTransient(Throwable e) {
        return e instanceof IOException || e instanceof ServerErrorResponse;
    }

    private static String describe(Throwable e) {
        if (e instanceof ServerErrorResponse serverError) {
            return "status " + serverError.response.statusCode();
        }
        return e == null ? "unknown" : e.getClass().getSimpleName();
    }

    // Carries a 5xx through Resilience4j as a failure, so it is retried and counted, and lets the
    // last one out again as the plain response it was.
    private static final class ServerErrorResponse extends Exception {

        private final transient HttpResponse<String> response;

        private ServerErrorResponse(HttpResponse<String> response) {
            super("status " + response.statusCode(), null, false, false);
            this.response = response;
        }
    }
}
