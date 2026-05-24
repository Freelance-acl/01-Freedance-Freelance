package com.team01.freelance.contract.audit;

import java.util.List;

public interface ContractEventAuditRepository {
    void save(ContractEventDTO event);
    List<ContractEventDTO> findByContractId(Long contractId);
}
