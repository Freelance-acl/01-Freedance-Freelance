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
                "status", "ACTIVE",
                "preferences", Map.of(
                        "language", "en",
                        "notifications", Map.of(
                                "email", true,
                                "sms", false
                        ),
                        "timezone", "Africa/Cairo",
                        "profileVisibility", "PUBLIC",
                        "hourlyRateRange", Map.of(
                                "min", 300,
                                "max", 600
                        )
                )
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
                "budgetMax", 20000,
                "requirements", Map.of(
                        "requiredSkills", new String[]{"Java", "Spring Boot", "PostgreSQL"},
                        "experienceLevel", "SENIOR",
                        "estimatedDuration", 8,
                        "remoteAllowed", true,
                        "preferredTimezone", "GMT+2"
                )
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
                "submittedAt", "2026-05-06T14:30:00",
                "metadata", Map.of(
                        "approachSummary", "Microservices with Spring Boot",
                        "relevantExperience", "5 years in similar projects",
                        "toolsProposed", new String[]{"IntelliJ", "Docker", "GitHub"},
                        "availabilityStart", "2026-04-01",
                        "portfolioLinks", new String[]{"https://portfolio.example.com/project1"}
                )
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
                "startDate", "2026-05-06T14:30:00",
                "metadata", Map.of(
                        "paymentTerms", "MILESTONE",
                        "revisionLimit", 3,
                        "ndaSigned", true,
                        "weeklyHoursExpected", 40,
                        "progressPercentage", 65,
                        "lastActivityDate", "2026-03-15"
                )
        ));

        Map<String, Object> payoutTransactionDetails = new LinkedHashMap<>();
        payoutTransactionDetails.put("gatewayResponse", "approved");
        payoutTransactionDetails.put("accountLastFour", "9876");
        payoutTransactionDetails.put("receiptUrl", "https://receipts.example.com/abc");
        payoutTransactionDetails.put("failureReason", null);

        Map<String, Object> payoutSampleBody = new LinkedHashMap<>();
        payoutSampleBody.put("contractId", 1);
        payoutSampleBody.put("freelancerId", 22);
        payoutSampleBody.put("amount", 140.5);
        payoutSampleBody.put("method", "BANK_TRANSFER");
        payoutSampleBody.put("status", "COMPLETED");
        payoutSampleBody.put("transactionDetails", payoutTransactionDetails);

        Map<String, Object> payout = new LinkedHashMap<>();
        payout.put("url", "http://localhost:8085/api/payouts");
        payout.put("method", "POST");
        payout.put("sampleBody", payoutSampleBody);

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
                "For job budgets, preferred keys are budgetMin and budgetMax.",
                "Payout API also accepts contract_id, conractId (typo alias), and freelancer_id."
        });

        return ResponseEntity.ok(body);
    }
}
