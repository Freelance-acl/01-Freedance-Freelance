package com.team01.freelance.contract.audit;

import java.time.LocalDateTime;
import java.util.Map;

public record ContractEventDTO(
        Long contractId,
        String action,
        LocalDateTime timestamp,
        Map<String, Object> details
) {
}
