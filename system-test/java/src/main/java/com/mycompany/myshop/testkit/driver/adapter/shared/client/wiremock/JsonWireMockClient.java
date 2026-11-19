package com.mycompany.myshop.testkit.driver.adapter.shared.client.wiremock;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.VerificationException;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.mycompany.myshop.testkit.common.Result;
import wiremock.com.fasterxml.jackson.databind.ObjectMapper;
import wiremock.com.fasterxml.jackson.databind.SerializationFeature;
import wiremock.com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

public class JsonWireMockClient {
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";
    private static final ExecutorService VIRTUAL_THREAD_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();
    // Admin calls to a Cloud Run stub service occasionally fail in Google's front end before they
    // reach the container (a 500 or 502 the container never logged, or a request that reads out). Retry those a bounded number of times.
    private static final int MAX_ADMIN_ATTEMPTS = 4;
    private static final long INITIAL_BACKOFF_MILLIS = 1000;
    private static final Pattern RETRYABLE_STATUS = Pattern.compile("but was (500|502|503|504)\\b");
    private final ObjectMapper objectMapper;

    private final WireMock wireMock;
    private final String adminUrl;
    private final List<UUID> stubIds = new ArrayList<>();

    public JsonWireMockClient(String baseUrl) {
        var uri = URI.create(baseUrl);
        this.wireMock = createWireMock(uri);
        this.adminUrl = uri.getScheme() + "://" + uri.getAuthority() + "/__admin/mappings";
        this.objectMapper = createObjectMapper();
    }

    // Honour the base URL's scheme and default port: in the cloud the stubs are an HTTPS
    // Cloud Run service with no explicit port (e.g. https://<svc>.run.app/erp), so the
    // admin API must be called over https on 443, not http on the URI's -1 "no port".
    // Only the origin is used: the WireMock admin API lives at /__admin on the root,
    // whatever stub path prefix (/erp, /clock, /tax) the base URL carries.
    private static WireMock createWireMock(URI url) {
        var scheme = url.getScheme() != null ? url.getScheme() : "http";
        var explicitPort = url.getPort();
        var builder = WireMock.create()
            .scheme(scheme)
            .host(url.getHost())
            .port(explicitPort != -1 ? explicitPort : defaultPort(scheme));
        if (explicitPort == -1) {
            // Keep the Host header portless, as a browser/curl would send it to the front end.
            builder.hostHeader(url.getHost());
        }
        return builder.build();
    }

    private static int defaultPort(String scheme) {
        return "https".equalsIgnoreCase(scheme) ? 443 : 80;
    }

    private static ObjectMapper createObjectMapper() {
        var mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    public <T> Result<Void, String> stubGet(String path, int statusCode, T response) {
        return runOnVirtualThread(() -> performStubRegistration(path, statusCode, response, WireMock::get, "GET"));
    }

    public <T> Result<Void, String> stubPost(String path, int statusCode, T response) {
        return runOnVirtualThread(() -> performStubRegistration(path, statusCode, response, WireMock::post, "POST"));
    }

    public <T> Result<Void, String> stubPut(String path, int statusCode, T response) {
        return runOnVirtualThread(() -> performStubRegistration(path, statusCode, response, WireMock::put, "PUT"));
    }

    public <T> Result<Void, String> stubDelete(String path, int statusCode, T response) {
        return runOnVirtualThread(() -> performStubRegistration(path, statusCode, response, WireMock::delete, "DELETE"));
    }

    public Result<Void, String> stubGet(String path, int statusCode) {
        return runOnVirtualThread(() -> performStubRegistrationWithoutBody(path, statusCode, WireMock::get, "GET"));
    }

    public Result<Void, String> stubPost(String path, int statusCode) {
        return runOnVirtualThread(() -> performStubRegistrationWithoutBody(path, statusCode, WireMock::post, "POST"));
    }

    public Result<Void, String> stubPut(String path, int statusCode) {
        return runOnVirtualThread(() -> performStubRegistrationWithoutBody(path, statusCode, WireMock::put, "PUT"));
    }

    public Result<Void, String> stubDelete(String path, int statusCode) {
        return runOnVirtualThread(() -> performStubRegistrationWithoutBody(path, statusCode, WireMock::delete, "DELETE"));
    }

    /**
     * Stubs every GET whose path matches {@code pathRegex} with a bodyless {@code statusCode}, at the
     * given priority (lower wins; stubs registered without one default to 5). Used to take a whole
     * external system down, so it outranks any per-resource stub registered in the same scenario.
     */
    public Result<Void, String> stubGetMatching(String pathRegex, int statusCode, int priority) {
        return stubGetMatchingWithDelay(pathRegex, statusCode, 0, priority);
    }

    /**
     * Like {@link #stubGetMatching}, but every matching GET answers only after {@code delayMilliseconds}.
     * Used to make a whole external system slow, so the system's outbound timeout is what ends the call.
     */
    public Result<Void, String> stubGetMatchingWithDelay(String pathRegex, int statusCode, int delayMilliseconds, int priority) {
        return runOnVirtualThread(() -> {
            register("GET " + pathRegex, WireMock.get(urlPathMatching(pathRegex))
                .atPriority(priority)
                .willReturn(aResponse()
                    .withStatus(statusCode)
                    .withFixedDelay(delayMilliseconds)));

            var registered = allStubMappings().stream()
                .anyMatch(m -> "GET".equals(m.getRequest().getMethod().getName())
                    && pathRegex.equals(m.getRequest().getUrlPathPattern()));

            if (!registered) {
                return Result.failure("Failed to register stub for GET " + pathRegex);
            }

            return Result.success();
        });
    }

    private Result<Void, String> runOnVirtualThread(Supplier<Result<Void, String>> work) {
        try {
            // Execute WireMock operations on virtual thread for non-blocking I/O
            return VIRTUAL_THREAD_EXECUTOR.submit(work::get).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to register stub", e);
        } catch (ExecutionException e) {
            var cause = e.getCause();
            throw new IllegalStateException("Failed to register stub", cause != null ? cause : e);
        }
    }

    private <T> Result<Void, String> performStubRegistration(
        String path,
        int statusCode,
        T response,
        Function<UrlPathPattern, MappingBuilder> methodBuilder,
        String methodName
    ) {
        var responseBody = serialize(response);

        register(methodName + " " + path, methodBuilder.apply(urlPathEqualTo(path))
            .willReturn(aResponse()
                .withStatus(statusCode)
                .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                .withBody(responseBody)));

        var registered = allStubMappings().stream()
            .anyMatch(m -> methodName.equals(m.getRequest().getMethod().getName())
                && path.equals(m.getRequest().getUrlPath()));

        if (!registered) {
            return Result.failure("Failed to register stub for " + methodName + " " + path);
        }

        return Result.success();
    }

    private Result<Void, String> performStubRegistrationWithoutBody(
        String path,
        int statusCode,
        Function<UrlPathPattern, MappingBuilder> methodBuilder,
        String methodName
    ) {
        register(methodName + " " + path, methodBuilder.apply(urlPathEqualTo(path))
            .willReturn(aResponse()
                .withStatus(statusCode)));

        var registered = allStubMappings().stream()
            .anyMatch(m -> methodName.equals(m.getRequest().getMethod().getName())
                && path.equals(m.getRequest().getUrlPath()));

        if (!registered) {
            return Result.failure("Failed to register stub for " + methodName + " " + path);
        }

        return Result.success();
    }

    /**
     * Registers the mapping under a client-chosen id, retrying transient admin failures. Before each
     * retry it checks whether the failed attempt reached the server after all, so a mapping is never
     * added twice.
     */
    private void register(String description, MappingBuilder mapping) {
        var id = UUID.randomUUID();
        mapping.withId(id);
        withRetry("register stub " + description + " (id " + id + ")", attempt -> {
            if (attempt == 1 || !isRegistered(id)) {
                wireMock.register(mapping);
            }
            return null;
        });
        stubIds.add(id);
    }

    private boolean isRegistered(UUID id) {
        return wireMock.allStubMappings().getMappings().stream().anyMatch(m -> id.equals(m.getId()));
    }

    private List<StubMapping> allStubMappings() {
        return withRetry("list stubs", attempt -> wireMock.allStubMappings().getMappings());
    }

    private <R> R withRetry(String description, IntFunction<R> call) {
        Throwable last = null;
        for (var attempt = 1; attempt <= MAX_ADMIN_ATTEMPTS; attempt++) {
            if (attempt > 1) {
                sleep(INITIAL_BACKOFF_MILLIS << (attempt - 2));
            }
            try {
                return call.apply(attempt);
            } catch (Exception | VerificationException e) {
                if (!isTransient(e)) {
                    throw e;
                }
                last = e;
                System.err.printf("WireMock admin call failed (attempt %d/%d): %s at %s: %s%n",
                    attempt, MAX_ADMIN_ATTEMPTS, description, adminUrl, e);
            }
        }
        throw new IllegalStateException("WireMock admin call failed after " + MAX_ADMIN_ATTEMPTS + " attempts: "
            + description + " at " + adminUrl + ": " + last, last);
    }

    // A 500/502/503/504 from the front end, or a transport failure (connect/read timeout, reset connection).
    // The WireMock client rethrows its IOExceptions unchecked, so the cause chain is walked.
    private static boolean isTransient(Throwable e) {
        for (var t = e; t != null; t = t.getCause()) {
            if (t instanceof VerificationException && t.getMessage() != null
                && RETRYABLE_STATUS.matcher(t.getMessage()).find()) {
                return true;
            }
            if (t instanceof IOException) {
                return true;
            }
        }
        return false;
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while retrying a WireMock admin call", e);
        }
    }

    /** Removes the stubs this client registered with {@link #stubGetMatching} for {@code pathRegex}. */
    public Result<Void, String> removeStubsMatching(String pathRegex) {
        return runOnVirtualThread(() -> {
            var matching = allStubMappings().stream()
                .filter(m -> stubIds.contains(m.getUuid()) && pathRegex.equals(m.getRequest().getUrlPathPattern()))
                .toList();
            for (var mapping : matching) {
                wireMock.removeStubMapping(mapping.getUuid());
                stubIds.remove(mapping.getUuid());
            }
            return Result.success();
        });
    }

    public void removeStubs() {
        for (var id : stubIds) {
            wireMock.removeStubMapping(id);
        }
        stubIds.clear();
    }

    private String serialize(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (wiremock.com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize object", ex);
        }
    }
}
