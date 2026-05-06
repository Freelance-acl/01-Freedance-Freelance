package com.team01.freelance.wallet.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/instructions")
public class ApiInstructionsController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> getInstructions() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", "API quick-start instructions");
        body.put("baseHost", "http://localhost");

        Map<String, Object> user = new LinkedHashMap<>();
        user.put("url", "http://localhost:8081/api/users");
        user.put("method", "POST");
        user.put("sampleBody", Map.of(
                "name", "Youssef1122",
                "email", "youssef1@x.x",
                "password", "youssef1",
                "phone", "+201550830082",
                "role", "ADMIN",
                "status", "ACTIVE"
        ));

        Map<String, Object> job = new LinkedHashMap<>();
        job.put("url", "http://localhost:8082/api/jobs");
        job.put("method", "POST");
        job.put("sampleBody", Map.of(
                "clientId", 1,
                "title", "Software Developer",
                "description", "develops software",
                "category", "WEB_DEV",
                "status", "IN_PROGRESS",
                "budgetMin", 10000,
                "budgetMax", 20000
        ));

        Map<String, Object> proposal = new LinkedHashMap<>();
        proposal.put("url", "http://localhost:8083/api/proposals");
        proposal.put("method", "POST");
        proposal.put("sampleBody", Map.of(
                "jobId", 1,
                "freelancerId", 20,
                "coverLetter", "freelancer cover letter",
                "bidAmount", 100.0,
                "estimatedDays", 12,
                "status", "ACCEPTED",
                "submittedAt", "2026-05-06T14:30:00"
        ));

        Map<String, Object> contract = new LinkedHashMap<>();
        contract.put("url", "http://localhost:8084/api/contracts");
        contract.put("method", "POST");
        contract.put("sampleBody", Map.of(
                "jobId", 1,
                "freelancerId", 20,
                "clientId", 1,
                "proposalId", 1,
                "agreedAmount", 150,
                "status", "COMPLETED",
                "startDate", "2026-05-06T14:30:00"
        ));

        Map<String, Object> payout = new LinkedHashMap<>();
        payout.put("url", "http://localhost:8085/api/payouts");
        payout.put("method", "POST");
        payout.put("sampleBody", Map.of(
                "contractId", 1,
                "freelancerId", 22,
                "amount", 140.5,
                "method", "BANK_TRANSFER",
                "status", "COMPLETED"
        ));

        Map<String, Object> endpoints = new LinkedHashMap<>();
        endpoints.put("user", user);
        endpoints.put("job", job);
        endpoints.put("proposal", proposal);
        endpoints.put("contract", contract);
        endpoints.put("payout", payout);
        body.put("endpoints", endpoints);

        body.put("notes", new String[]{
                "Create records in order: User -> Job -> Proposal -> Contract -> Payout.",
                "Use freelancerId (not freeLancerId).",
                "Use contractId (not conractId).",
                "For job budgets, preferred keys are budgetMin and budgetMax."
        });

        return ResponseEntity.ok(body);
    }
}
