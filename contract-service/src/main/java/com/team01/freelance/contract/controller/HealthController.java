package com.team01.freelance.contract.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contracts")
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    @GetMapping
    public ResponseEntity<String> index() {
        return ResponseEntity.ok("Welcome to Freelance Contract Service");
    }
}

