package com.team01.freelance.contract.messaging.publishers;

import com.team01.freelance.contract.config.RabbitMQConfig;
import com.team01.freelance.contract.model.Contract;
import com.team01.freelance.contract.model.ContractStatus;
import com.team01.freelance.contracts.events.ContractCancelledEvent;
import com.team01.freelance.contracts.events.ContractCreatedEvent;
import com.team01.freelance.contracts.events.ContractStatusChangedEvent;
import java.math.BigDecimal;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class ContractEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public ContractEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishContractCreated(Contract contract) {
        ContractCreatedEvent event = new ContractCreatedEvent(
                contract.getId(),
                contract.getProposalId(),
                contract.getJobId(),
                contract.getFreelancerId(),
                BigDecimal.valueOf(contract.getAgreedAmount())
        );
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.CONTRACT_EXCHANGE,
                ContractCreatedEvent.ROUTING_KEY,
                event
        );
    }

    public void publishContractStatusChanged(Long contractId, ContractStatus oldStatus, ContractStatus newStatus) {
        ContractStatusChangedEvent event = new ContractStatusChangedEvent(
                contractId,
                oldStatus == null ? null : oldStatus.name(),
                newStatus == null ? null : newStatus.name()
        );
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.CONTRACT_EXCHANGE,
                ContractStatusChangedEvent.ROUTING_KEY,
                event
        );
    }

    public void publishContractCancelled(Contract contract) {
        ContractCancelledEvent event = new ContractCancelledEvent(contract.getId(), contract.getProposalId());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.CONTRACT_EXCHANGE,
                ContractCancelledEvent.ROUTING_KEY,
                event
        );
    }
}
