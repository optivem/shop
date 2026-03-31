package com.optivem.starter.monolith.controllers.api;

import com.optivem.starter.monolith.models.GreetingResponse;
import com.optivem.starter.monolith.services.GreetingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class GreetingApiController {

    private final GreetingService greetingService;

    public GreetingApiController(GreetingService greetingService) {
        this.greetingService = greetingService;
    }

    @GetMapping("/greeting")
    public GreetingResponse getGreeting() {
        return greetingService.getGreeting();
    }
}
