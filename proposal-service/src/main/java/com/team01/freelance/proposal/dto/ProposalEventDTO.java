package com.team01.freelance.proposal.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record ProposalEventDTO(
        Long proposalId,
        String action,
        LocalDateTime timestamp,
        Map<String, Object> details) {
}
