package com.optivem.starter.monolith.controllers.api;

import com.optivem.starter.monolith.exceptions.GreetingNotAvailableException;
import com.optivem.starter.monolith.models.GreetingResponse;
import com.optivem.starter.monolith.services.GreetingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GreetingApiController.class)
class GreetingApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GreetingService greetingService;

    @Test
    void getGreetingReturnsGreetingResponse() throws Exception {
        GreetingResponse response = new GreetingResponse(
                "Good morning", "test message", "08:30", 4);
        when(greetingService.getGreeting()).thenReturn(response);

        mockMvc.perform(get("/api/greeting"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.greeting").value("Good morning"))
                .andExpect(jsonPath("$.message").value("test message"))
                .andExpect(jsonPath("$.localTime").value("08:30"))
                .andExpect(jsonPath("$.estimatedReadingTimeSeconds").value(4));
    }

    @Test
    void getGreetingDuringNightReturns400() throws Exception {
        when(greetingService.getGreeting()).thenThrow(
                new GreetingNotAvailableException("Greeting service is not available during night hours"));

        mockMvc.perform(get("/api/greeting"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GREETING_NOT_AVAILABLE"))
                .andExpect(jsonPath("$.message").value(
                        "Greeting service is not available during night hours"));
    }
}
