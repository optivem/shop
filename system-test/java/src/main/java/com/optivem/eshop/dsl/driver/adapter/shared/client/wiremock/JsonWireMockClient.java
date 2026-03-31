package com.optivem.eshop.dsl.driver.adapter.shared.client.wiremock;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import com.optivem.eshop.dsl.common.Result;
import wiremock.com.fasterxml.jackson.databind.ObjectMapper;
import wiremock.com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

public class JsonWireMockClient {
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";
    private static final ExecutorService VIRTUAL_THREAD_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();
    private final ObjectMapper objectMapper;

    private final WireMock wireMock;

    public JsonWireMockClient(String baseUrl) {
        var url = URI.create(baseUrl);
        this.wireMock = new WireMock(url.getHost(), url.getPort());
        this.objectMapper = createObjectMapper();
    }

    private static ObjectMapper createObjectMapper() {
        var mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
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

        wireMock.register(methodBuilder.apply(urlPathEqualTo(path))
            .willReturn(aResponse()
                .withStatus(statusCode)
                .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                .withBody(responseBody)));

        var mappings = wireMock.allStubMappings();
        var registered = mappings.getMappings().stream()
            .anyMatch(m -> methodName.equals(m.getRequest().getMethod().getName())
                && m.getRequest().getUrlPath().equals(path));

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
        wireMock.register(methodBuilder.apply(urlPathEqualTo(path))
            .willReturn(aResponse()
                .withStatus(statusCode)));

        var mappings = wireMock.allStubMappings();
        var registered = mappings.getMappings().stream()
            .anyMatch(m -> methodName.equals(m.getRequest().getMethod().getName())
                && m.getRequest().getUrlPath().equals(path));

        if (!registered) {
            return Result.failure("Failed to register stub for " + methodName + " " + path);
        }

        return Result.success();
    }

    private String serialize(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (wiremock.com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize object", ex);
        }
    }
}


