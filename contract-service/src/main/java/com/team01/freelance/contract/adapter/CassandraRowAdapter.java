package com.team01.freelance.contract.adapter;

import com.team01.freelance.contract.dto.ContractMilestoneDTO;
import com.team01.freelance.contract.milestone.ContractMilestoneEvent;
import org.springframework.stereotype.Component;

@Component
public class CassandraRowAdapter {

    public ContractMilestoneDTO adapt(ContractMilestoneEvent event) {
        return ContractMilestoneDTO.builder()
                .timestamp(event.getTimestamp())
                .milestoneOrder(event.getMilestoneOrder())
                .status(event.getStatus() != null ? event.getStatus().name() : null)
                .recordedBy(event.getRecordedBy())
                .notes(event.getNotes())
                .build();
    }
}
