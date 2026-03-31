package com.optivem.starter.monolith.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.optivem.starter.monolith.exceptions.GreetingNotAvailableException;
import com.optivem.starter.monolith.models.GreetingResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Service
public class GreetingService {

    private static final int MORNING_START = 6;
    private static final int AFTERNOON_START = 12;
    private static final int EVENING_START = 18;
    private static final int NIGHT_START = 20;
    private static final int SECONDS_PER_WORD = 2;
    private static final String FALLBACK_MESSAGE = "Have a great day!";

    private final Clock clock;
    private final RestTemplate restTemplate;
    private final String quotesApiHost;

    public GreetingService(Clock clock, RestTemplateBuilder restTemplateBuilder,
                           @Value("${quotes.api.host}") String quotesApiHost) {
        this.clock = clock;
        this.quotesApiHost = quotesApiHost;
        this.restTemplate = restTemplateBuilder
                .requestFactory(() -> {
                    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
                    Duration timeout = Duration.ofSeconds(10);
                    factory.setConnectTimeout(timeout);
                    factory.setReadTimeout(timeout);
                    return factory;
                })
                .build();
    }

    public GreetingResponse getGreeting() {
        LocalTime now = LocalTime.now(clock);
        int hour = now.getHour();

        if (hour >= NIGHT_START || hour < MORNING_START) {
            throw new GreetingNotAvailableException(
                "Greeting service is not available during night hours (20:00-06:00). "
                    + "Current time: " + now.format(DateTimeFormatter.ofPattern("HH:mm"))
            );
        }

        String greeting;
        if (hour < AFTERNOON_START) {
            greeting = "Good morning";
        } else if (hour < EVENING_START) {
            greeting = "Good afternoon";
        } else {
            greeting = "Good evening";
        }

        String message = fetchMessage(hour);
        int estimatedReadingTimeSeconds = countWords(message) * SECONDS_PER_WORD;
        String localTime = now.format(DateTimeFormatter.ofPattern("HH:mm"));

        return new GreetingResponse(greeting, message, localTime, estimatedReadingTimeSeconds);
    }

    private String fetchMessage(int hour) {
        String url = quotesApiHost + "/posts/" + hour;
        try {
            JsonNode post = restTemplate.getForObject(url, JsonNode.class);
            if (post != null && post.has("title")) {
                return post.get("title").asText();
            }
            return FALLBACK_MESSAGE;
        } catch (RestClientException e) {
            return FALLBACK_MESSAGE;
        }
    }

    private int countWords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }
}
