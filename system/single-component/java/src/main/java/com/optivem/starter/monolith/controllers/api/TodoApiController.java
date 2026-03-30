package com.optivem.starter.monolith.controllers.api;

import com.optivem.starter.monolith.models.Todo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

@RestController
@RequestMapping("/api")
public class TodoApiController {

    @Value("${todos.api.host}")
    private String todosApiHost;

    private final RestTemplate restTemplate;

    public TodoApiController(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .requestFactory(() -> {
                    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
                    Duration timeout = Duration.ofSeconds(30);
                    factory.setConnectTimeout(timeout);
                    factory.setReadTimeout(timeout);
                    return factory;
                })
                .build();
    }

    @GetMapping("/todos/{id}")
    public Todo getTodo(@PathVariable("id") int id) {
        String url = todosApiHost + "/todos/" + id;

        int maxRetries = 3;
        int retryDelayMs = 1000;

        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                Todo todo = restTemplate.getForObject(url, Todo.class);
                if (todo == null) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Todo not found");
                }
                return todo;
            } catch (RestClientException e) {
                if (attempt == maxRetries - 1) {
                    throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "External API is unavailable after " + maxRetries + " attempts: " + e.getMessage(),
                        e
                    );
                }

                try {
                    Thread.sleep(retryDelayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Retry interrupted",
                        ie
                    );
                }
            }
        }

        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error");
    }
}