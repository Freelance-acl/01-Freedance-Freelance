package com.team01.freelance.contract.observer;

import com.team01.freelance.contract.adapter.MongoDocumentAdapter;
import com.team01.freelance.contract.audit.ContractEventAuditRepository;
import com.team01.freelance.contract.audit.EventFactory;
import org.springframework.stereotype.Component;

@Component
public class MongoEventLogger implements EntityObserver {

    private final EventFactory eventFactory;
    private final MongoDocumentAdapter adapter;
    private final ContractEventAuditRepository auditRepository;

    public MongoEventLogger(
            EventFactory eventFactory,
            MongoDocumentAdapter adapter,
            ContractEventAuditRepository auditRepository
    ) {
        this.eventFactory = eventFactory;
        this.adapter = adapter;
        this.auditRepository = auditRepository;
    }

    @Override
    public void onEvent(String eventType, Object payload) {
        auditRepository.save(adapter.adapt(eventFactory.createContractEvent(eventType, payload)));
    }
}
