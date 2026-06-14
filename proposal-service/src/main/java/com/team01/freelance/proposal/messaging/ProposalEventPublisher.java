package com.team01.freelance.proposal.messaging;

import com.team01.freelance.contract.model.Contract;
import com.team01.freelance.contracts.events.ProposalCompletedEvent;
import com.team01.freelance.proposal.config.RabbitMQConfig;
import com.team01.freelance.proposal.model.Proposal;
import java.math.BigDecimal;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class ProposalEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public ProposalEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishProposalCompleted(Proposal proposal, Contract contract) {
        ProposalCompletedEvent event = new ProposalCompletedEvent(
                proposal.getId(),
                proposal.getJobId(),
                proposal.getFreelancerId(),
                contract.getId(),
                BigDecimal.valueOf(contract.getAgreedAmount())
        );
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.PROPOSAL_EXCHANGE,
                ProposalCompletedEvent.ROUTING_KEY,
                event
        );
    }
}
