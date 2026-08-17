package com.mycompany.myshop.backend.api.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> checkHealth() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
