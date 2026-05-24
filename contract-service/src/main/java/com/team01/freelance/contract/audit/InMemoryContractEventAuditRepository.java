package com.team01.freelance.contract.audit;

import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
public class InMemoryContractEventAuditRepository implements ContractEventAuditRepository {

    private final List<ContractEventDTO> store = new CopyOnWriteArrayList<>();

    @Override
    public void save(ContractEventDTO event) {
        store.add(event);
    }

    @Override
    public List<ContractEventDTO> findByContractId(Long contractId) {
        return store.stream().filter(event -> contractId.equals(event.contractId())).toList();
    }

    public List<ContractEventDTO> findAll() {
        return new ArrayList<>(store);
    }
}
