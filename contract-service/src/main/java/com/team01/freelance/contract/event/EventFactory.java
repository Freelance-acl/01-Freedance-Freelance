package com.team01.freelance.contract.event;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class EventFactory {

    public MongoEvent createEvent(EventType type, Map<String, Object> params) {
        if (type != EventType.CONTRACT) {
            throw new IllegalArgumentException("Unsupported event type for contract-service: " + type);
        }

        ContractEvent event = new ContractEvent();
        event.setContractId(toLong(params.getOrDefault("contractId", 0L)));
        event.setAction(String.valueOf(params.get("action")));
        event.setTimestamp(LocalDateTime.now());
        Object details = params.get("details");
        if (details instanceof Map<?, ?> detailsMap) {
            Map<String, Object> normalizedDetails = new LinkedHashMap<>();
            detailsMap.forEach((key, value) -> normalizedDetails.put(String.valueOf(key), value));
            event.setDetails(normalizedDetails);
        } else {
            event.setDetails(new LinkedHashMap<>());
        }
        return event;
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }
}
