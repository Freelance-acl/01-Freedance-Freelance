package com.team01.freelance.contract.milestone;

import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
public class InMemoryContractMilestoneTimelineRepository implements ContractMilestoneTimelineRepository {

    private final List<ContractMilestoneEvent> events = new CopyOnWriteArrayList<>();

    @Override
    public void save(ContractMilestoneEvent event) {
        events.add(event);
    }

    @Override
    public List<ContractMilestoneEvent> findByContractId(Long contractId, LocalDateTime startTime, LocalDateTime endTime) {
        return events.stream()
                .filter(event -> contractId.equals(event.contractId()))
                .filter(event -> startTime == null || !event.timestamp().isBefore(startTime))
                .filter(event -> endTime == null || !event.timestamp().isAfter(endTime))
                .sorted((a, b) -> b.timestamp().compareTo(a.timestamp()))
                .toList();
    }
}
