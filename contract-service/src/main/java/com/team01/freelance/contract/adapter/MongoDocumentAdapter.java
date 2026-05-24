package com.team01.freelance.contract.adapter;

import com.team01.freelance.contract.audit.ContractEvent;
import com.team01.freelance.contract.audit.ContractEventDTO;
import org.springframework.stereotype.Component;

@Component
public class MongoDocumentAdapter {

    public ContractEventDTO adapt(ContractEvent event) {
        return new ContractEventDTO(event.getContractId(), event.getAction(), event.getTimestamp(), event.getDetails());
    }
}
