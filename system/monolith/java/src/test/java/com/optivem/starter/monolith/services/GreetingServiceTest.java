package com.optivem.starter.monolith.services;

import com.optivem.starter.monolith.exceptions.GreetingNotAvailableException;
import com.optivem.starter.monolith.models.GreetingResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GreetingServiceTest {

    private static final ZoneId ZONE = ZoneId.of("UTC");
    private static final String API_HOST = "https://jsonplaceholder.typicode.com";

    private static Clock fixedClock(int hour) {
        Instant instant = LocalDate.of(2026, 3, 31)
                .atTime(LocalTime.of(hour, 30))
                .atZone(ZONE)
                .toInstant();
        return Clock.fixed(instant, ZONE);
    }

    @Test
    void getGreetingAt8amReturnsGoodMorning() {
        GreetingServiceTestHelper helper = new GreetingServiceTestHelper(fixedClock(8));
        helper.stubExternalApi(8, "morning title");

        GreetingResponse response = helper.service.getGreeting();

        assertEquals("Good morning", response.getGreeting());
        assertEquals("morning title", response.getMessage());
        assertEquals("08:30", response.getLocalTime());
    }

    @Test
    void getGreetingAt14pmReturnsGoodAfternoon() {
        GreetingServiceTestHelper helper = new GreetingServiceTestHelper(fixedClock(14));
        helper.stubExternalApi(14, "afternoon title");

        GreetingResponse response = helper.service.getGreeting();

        assertEquals("Good afternoon", response.getGreeting());
        assertEquals("afternoon title", response.getMessage());
        assertEquals("14:30", response.getLocalTime());
    }

    @Test
    void getGreetingAt19pmReturnsGoodEvening() {
        GreetingServiceTestHelper helper = new GreetingServiceTestHelper(fixedClock(19));
        helper.stubExternalApi(19, "evening title");

        GreetingResponse response = helper.service.getGreeting();

        assertEquals("Good evening", response.getGreeting());
        assertEquals("evening title", response.getMessage());
        assertEquals("19:30", response.getLocalTime());
    }

    @Test
    void getGreetingAt22pmThrowsException() {
        GreetingServiceTestHelper helper = new GreetingServiceTestHelper(fixedClock(22));

        assertThrows(GreetingNotAvailableException.class, () -> helper.service.getGreeting());
    }

    @Test
    void getGreetingAt3amThrowsException() {
        GreetingServiceTestHelper helper = new GreetingServiceTestHelper(fixedClock(3));

        assertThrows(GreetingNotAvailableException.class, () -> helper.service.getGreeting());
    }

    @Test
    void getGreetingCalculatesReadingTimeFromWordCount() {
        GreetingServiceTestHelper helper = new GreetingServiceTestHelper(fixedClock(10));
        helper.stubExternalApi(10, "one two three four five");

        GreetingResponse response = helper.service.getGreeting();

        assertEquals(10, response.getEstimatedReadingTimeSeconds());
    }

    @Test
    void getGreetingExternalServiceDownReturnsFallback() {
        GreetingServiceTestHelper helper = new GreetingServiceTestHelper(fixedClock(10));
        helper.stubExternalApiError(10);

        GreetingResponse response = helper.service.getGreeting();

        assertEquals("Good morning", response.getGreeting());
        assertEquals("Have a great day!", response.getMessage());
        assertEquals(8, response.getEstimatedReadingTimeSeconds());
    }

    private static class GreetingServiceTestHelper {
        final GreetingService service;
        final MockRestServiceServer mockServer;

        GreetingServiceTestHelper(Clock clock) {
            org.springframework.boot.web.client.RestTemplateBuilder builder =
                    new org.springframework.boot.web.client.RestTemplateBuilder();
            service = new GreetingService(clock, builder, API_HOST);

            try {
                java.lang.reflect.Field restTemplateField =
                        GreetingService.class.getDeclaredField("restTemplate");
                restTemplateField.setAccessible(true);
                org.springframework.web.client.RestTemplate restTemplate =
                        (org.springframework.web.client.RestTemplate) restTemplateField.get(service);
                mockServer = MockRestServiceServer.createServer(restTemplate);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        void stubExternalApi(int hour, String title) {
            mockServer.expect(requestTo(API_HOST + "/posts/" + hour))
                    .andRespond(withSuccess(
                            "{\"title\": \"" + title + "\"}",
                            MediaType.APPLICATION_JSON));
        }

        void stubExternalApiError(int hour) {
            mockServer.expect(requestTo(API_HOST + "/posts/" + hour))
                    .andRespond(withServerError());
        }
    }
}
