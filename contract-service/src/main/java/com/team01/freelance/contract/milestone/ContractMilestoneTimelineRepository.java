package com.team01.freelance.contract.milestone;

import java.time.LocalDateTime;
import java.util.List;

public interface ContractMilestoneTimelineRepository {
    void save(ContractMilestoneEvent event);
    List<ContractMilestoneEvent> findByContractId(Long contractId, LocalDateTime startTime, LocalDateTime endTime);
}
