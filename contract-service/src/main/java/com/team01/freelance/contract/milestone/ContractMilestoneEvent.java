package com.team01.freelance.contract.milestone;

import java.time.LocalDateTime;

public record ContractMilestoneEvent(
        Long contractId,
        LocalDateTime timestamp,
        Integer milestoneOrder,
        ContractMilestoneStatus status,
        String recordedBy,
        String notes
) {
}
