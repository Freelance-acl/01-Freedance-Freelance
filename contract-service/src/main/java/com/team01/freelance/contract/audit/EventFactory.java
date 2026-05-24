package com.team01.freelance.contract.audit;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class EventFactory {

    @SuppressWarnings("unchecked")
    public ContractEvent createContractEvent(String eventType, Object payload) {
        Map<String, Object> mapPayload = payload instanceof Map<?, ?>
                ? (Map<String, Object>) payload
                : Collections.emptyMap();
        Long contractId = toLong(mapPayload.get("contractId"));
        Map<String, Object> details = new LinkedHashMap<>(mapPayload);
        details.remove("contractId");
        return new ContractEvent(contractId, eventType, LocalDateTime.now(), details);
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }
}
