package com.mycompany.myshop.backend.api.controller;

import java.util.Map;

import com.mycompany.myshop.backend.core.services.StatusService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/status")
public class StatusController {

    private final StatusService statusService;

    public StatusController(StatusService statusService) {
        this.statusService = statusService;
    }

    @GetMapping("/{system}")
    public ResponseEntity<Map<String, String>> getStatus(@PathVariable String system) {
        return statusService.isUp(system)
                .map(up -> ResponseEntity
                        .status(up ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
                        .body(Map.of("status", up ? "UP" : "DOWN")))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
