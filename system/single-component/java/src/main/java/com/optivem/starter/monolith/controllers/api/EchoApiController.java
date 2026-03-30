package com.optivem.starter.monolith.controllers.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class EchoApiController {

    @GetMapping("/echo")
    public String echo() {
        return "Echo";
    }
}