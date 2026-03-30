package com.optivem.starter.systemtest.e2etests;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

class ApiE2eTest {

    @Test
    void getTodos_shouldReturnTodoWithExpectedFormat() throws Exception {
        // DISCLAIMER: This is an example of a badly written test
        // which unfortunately simulates real-life software test projects.
        // This is the starting point for our Starter exercises.

        // Arrange
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("http://localhost:8080/api/todos/4"))
                .GET()
                .build();

        // Act
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Assert
        assertEquals(200, response.statusCode());

        String responseBody = response.body();

        // Verify JSON structure contains expected fields
        assertTrue(responseBody.contains("\"userId\""), "Response should contain userId field");
        assertTrue(responseBody.contains("\"id\""), "Response should contain id field");
        assertTrue(responseBody.contains("\"title\""), "Response should contain title field");
        assertTrue(responseBody.contains("\"completed\""), "Response should contain completed field");

        // Verify the specific todo has id 4
        assertTrue(responseBody.contains("\"id\":4") || responseBody.contains("\"id\": 4"),
                   "Response should contain id with value 4");
    }
}
