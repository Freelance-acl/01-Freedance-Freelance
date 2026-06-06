package com.team01.freelance.wallet.repository;

import com.team01.freelance.wallet.event.PayoutAuditEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Collection;

public interface PayoutAuditEventRepository extends MongoRepository<PayoutAuditEvent, String> {
    List<PayoutAuditEvent> findByTimestampBetweenAndActionIn(LocalDateTime start, LocalDateTime end, Collection<String> actions);
}