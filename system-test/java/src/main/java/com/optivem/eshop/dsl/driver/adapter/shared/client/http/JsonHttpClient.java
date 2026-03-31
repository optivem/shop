package com.optivem.eshop.dsl.driver.adapter.shared.client.http;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.optivem.eshop.dsl.common.Closer;
import com.optivem.eshop.dsl.common.Result;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class JsonHttpClient<E> implements AutoCloseable {
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";
    private static final ExecutorService VIRTUAL_THREAD_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient;
    private final String baseUrl;
    private final Class<E> errorType;

    public JsonHttpClient(HttpClient httpClient, String baseUrl, Class<E> errorType, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.baseUrl = baseUrl;
        this.errorType = errorType;
        this.objectMapper = objectMapper;
    }

    public JsonHttpClient(String baseUrl, Class<E> errorType) {
        this(HttpClient.newHttpClient(), baseUrl, errorType, createObjectMapper());
    }

    private static ObjectMapper createObjectMapper() {
        var mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        // Enable case-insensitive property binding
        mapper.setConfig(mapper.getDeserializationConfig()
                .with(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES));
        return mapper;
    }

    @Override
    public void close() {
        Closer.close(httpClient);
    }

    // GET Methods

    public <T> Result<T, E> get(String path, Class<T> responseType) {
        var httpResponse = doGet(path);
        return getResultOrFailure(httpResponse, responseType);
    }

    public Result<Void, E> get(String path) {
        var httpResponse = doGet(path);
        return getResultOrFailure(httpResponse, Void.class);
    }
    
    // POST Methods
    
    public <T> Result<T, E> post(String path, Object request, Class<T> responseType) {
        var httpResponse = doPost(path, request);
        return getResultOrFailure(httpResponse, responseType);
    }
    
    public Result<Void, E> post(String path, Object request) {
        var httpResponse = doPost(path, request);
        return getResultOrFailure(httpResponse, Void.class);
    }

    public <T> Result<T, E> post(String path, Class<T> responseType) {
        var httpResponse = doPost(path);
        return getResultOrFailure(httpResponse, responseType);
    }

    public Result<Void, E> post(String path) {
        var httpResponse = doPost(path);
        return getResultOrFailure(httpResponse, Void.class);
    }

    // PUT Methods

    public <T> Result<T, E> put(String path, Object request, Class<T> responseType) {
        var httpResponse = doPut(path, request);
        return getResultOrFailure(httpResponse, responseType);
    }

    public Result<Void, E> put(String path, Object request) {
        var httpResponse = doPut(path, request);
        return getResultOrFailure(httpResponse, Void.class);
    }

    // DELETE Methods

    public <T> Result<T, E> delete(String path, Class<T> responseType) {
        var httpResponse = doDelete(path);
        return getResultOrFailure(httpResponse, responseType);
    }

    public Result<Void, E> delete(String path) {
        var httpResponse = doDelete(path);
        return getResultOrFailure(httpResponse, Void.class);
    }

    // Private helper methods

    private URI getUri(String path) {
        try {
            return new URI(baseUrl + path);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("Failed to create URI for path: " + path, ex);
        }
    }

    private HttpResponse<String> doGet(String path) {
        var uri = getUri(path);
        var httpRequest = HttpRequest.newBuilder()
                .uri(uri)
                .GET()
                .build();

        return sendRequest(httpRequest);
    }

    private HttpResponse<String> doPost(String path, Object request) {
        var uri = getUri(path);
        var jsonBody = serializeRequest(request);

        var httpRequest = HttpRequest.newBuilder()
                .uri(uri)
                .header(CONTENT_TYPE, APPLICATION_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        return sendRequest(httpRequest);
    }

    private HttpResponse<String> doPost(String path) {
        var uri = getUri(path);

        var httpRequest = HttpRequest.newBuilder()
                .uri(uri)
                .header(CONTENT_TYPE, APPLICATION_JSON)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        return sendRequest(httpRequest);
    }

    private HttpResponse<String> doPut(String path, Object request) {
        var uri = getUri(path);
        var jsonBody = serializeRequest(request);

        var httpRequest = HttpRequest.newBuilder()
                .uri(uri)
                .header(CONTENT_TYPE, APPLICATION_JSON)
                .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        return sendRequest(httpRequest);
    }

    private HttpResponse<String> doDelete(String path) {
        var uri = getUri(path);

        var httpRequest = HttpRequest.newBuilder()
                .uri(uri)
                .DELETE()
                .build();

        return sendRequest(httpRequest);
    }

    private HttpResponse<String> sendRequest(HttpRequest httpRequest) {
        try {
            // Execute HTTP request on virtual thread for non-blocking I/O
            return VIRTUAL_THREAD_EXECUTOR.submit(() ->
                    httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString())
            ).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to send HTTP request", e);
        } catch (ExecutionException e) {
            var cause = e.getCause();
            throw new IllegalStateException("Failed to send HTTP request", cause != null ? cause : e);
        }
    }

    private String serializeRequest(Object request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize request object", ex);
        }
    }

    private <T> T readResponse(HttpResponse<String> httpResponse, Class<T> responseType) {
        try {
            var responseBody = httpResponse.body();
            return objectMapper.readValue(responseBody, responseType);
        } catch (com.fasterxml.jackson.core.JsonParseException ex) {
            var responseBody = httpResponse.body();
            var fallback = tryCreateErrorFromBody(responseType, responseBody, ex);
            if (fallback != null) {
                return fallback;
            }
            throw new IllegalStateException("Failed to deserialize response to " + responseType.getSimpleName() + ". Response body: " + responseBody, ex);
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new IllegalStateException("Failed to deserialize response to " + responseType.getSimpleName(), ex);
        }
    }

    @SuppressWarnings("java:S3011") // Reflection needed for error response fallback when JSON parsing fails
    private <T> T tryCreateErrorFromBody(Class<T> responseType, String responseBody, com.fasterxml.jackson.core.JsonParseException ex) {
        try {
            var constructor = responseType.getDeclaredConstructor();
            constructor.setAccessible(true);
            var instance = constructor.newInstance();
            var messageField = responseType.getDeclaredField("message");
            messageField.setAccessible(true);
            messageField.set(instance, responseBody);
            return instance;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private boolean isSuccessStatusCode(HttpResponse<String> httpResponse) {
        var statusCode = HttpStatus.valueOf(httpResponse.statusCode());
        return statusCode.is2xxSuccessful();
    }

    private <T> Result<T, E> getResultOrFailure(HttpResponse<String> httpResponse, Class<T> responseType) {
        if (!isSuccessStatusCode(httpResponse)) {
            var error = readResponse(httpResponse, errorType);
            return Result.failure(error);
        }

        if (responseType == Void.class || httpResponse.statusCode() == HttpStatus.NO_CONTENT.value()) {
            return Result.success(null);
        }

        var response = readResponse(httpResponse, responseType);
        return Result.success(response);
    }
}


