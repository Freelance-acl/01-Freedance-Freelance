package com.team01.freelance.wallet.adapter;

import com.team01.freelance.wallet.event.PayoutAuditEvent;
import com.team01.freelance.wallet.model.PayoutMethod;
import org.bson.Document;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class MongoDocumentAdapter {
    public PayoutAuditEvent adapt(Document source) {
        if (source == null) {
            return null;
        }

        PayoutAuditEvent event = new PayoutAuditEvent();

        if (source.getObjectId("_id") != null) {
            event.setId(source.getObjectId("_id").toHexString());
        }

        if (source.get("payoutId") != null) {
            event.setPayoutId(((Number) source.get("payoutId")).longValue());
        }

        event.setAction(source.getString("action"));

        Date timestamp = source.getDate("timestamp");
        if (timestamp != null) {
            event.setTimestamp(timestamp.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime());
        }

        String methodStr = source.getString("method");
        if (methodStr != null) {
            try {
                event.setMethod(PayoutMethod.valueOf(methodStr));
            } catch (IllegalArgumentException e) {
                event.setMethod(null);
            }
        }

        if (source.get("amount") != null) {
            event.setAmount(((Number) source.get("amount")).doubleValue());
        }

        Object detailsObj = source.get("details");
        if (detailsObj instanceof Map<?, ?> detailsMap) {
            Map<String, Object> convertedMap = new HashMap<>();
            for (Map.Entry<?, ?> entry : detailsMap.entrySet()) {
                convertedMap.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            event.setDetails(convertedMap);
        }

        return event;
    }
}