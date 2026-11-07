package com.mycompany.myshop.testkit.driver.adapter.shared.client.wiremock;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import com.mycompany.myshop.testkit.common.Result;
import wiremock.com.fasterxml.jackson.databind.ObjectMapper;
import wiremock.com.fasterxml.jackson.databind.SerializationFeature;
import wiremock.com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

public class JsonWireMockClient {
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";
    private static final ExecutorService VIRTUAL_THREAD_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();
    private final ObjectMapper objectMapper;

    private final WireMock wireMock;
    private final List<UUID> stubIds = new ArrayList<>();

    public JsonWireMockClient(String baseUrl) {
        this.wireMock = createWireMock(URI.create(baseUrl));
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
        return runOnVirtualThread(() -> {
            var stubMapping = wireMock.register(WireMock.get(urlPathMatching(pathRegex))
                .atPriority(priority)
                .willReturn(aResponse()
                    .withStatus(statusCode)));

            if (stubMapping != null && stubMapping.getUuid() != null) {
                stubIds.add(stubMapping.getUuid());
            }

            var registered = wireMock.allStubMappings().getMappings().stream()
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

        var stubMapping = wireMock.register(methodBuilder.apply(urlPathEqualTo(path))
            .willReturn(aResponse()
                .withStatus(statusCode)
                .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                .withBody(responseBody)));

        if (stubMapping != null && stubMapping.getUuid() != null) {
            stubIds.add(stubMapping.getUuid());
        }

        var mappings = wireMock.allStubMappings();
        var registered = mappings.getMappings().stream()
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
        var stubMapping = wireMock.register(methodBuilder.apply(urlPathEqualTo(path))
            .willReturn(aResponse()
                .withStatus(statusCode)));

        if (stubMapping != null && stubMapping.getUuid() != null) {
            stubIds.add(stubMapping.getUuid());
        }

        var mappings = wireMock.allStubMappings();
        var registered = mappings.getMappings().stream()
            .anyMatch(m -> methodName.equals(m.getRequest().getMethod().getName())
                && path.equals(m.getRequest().getUrlPath()));

        if (!registered) {
            return Result.failure("Failed to register stub for " + methodName + " " + path);
        }

        return Result.success();
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
