package com.team01.freelance.proposal.messaging.publishers;

import com.team01.freelance.contracts.events.ProposalAcceptedEvent;
import com.team01.freelance.contracts.events.ProposalCancelledEvent;
import com.team01.freelance.contracts.events.ProposalCompletedEvent;
import com.team01.freelance.contracts.events.ProposalWithdrawnEvent;
import com.team01.freelance.proposal.config.RabbitMQConfig;
import com.team01.freelance.proposal.dto.FeignContractDTO;
import com.team01.freelance.proposal.model.Proposal;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class ProposalEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ProposalEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public ProposalEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishProposalAccepted(Proposal proposal) {
        ProposalAcceptedEvent event = new ProposalAcceptedEvent(
                proposal.getId(),
                proposal.getJobId(),
                proposal.getFreelancerId(),
                BigDecimal.valueOf(proposal.getBidAmount())
        );
        publish(ProposalAcceptedEvent.ROUTING_KEY, event, proposal.getId());
    }

    public void publishProposalCompleted(Proposal proposal, FeignContractDTO contract) {
        ProposalCompletedEvent event = new ProposalCompletedEvent(
                proposal.getId(),
                proposal.getJobId(),
                proposal.getFreelancerId(),
                contract.getId(),
                BigDecimal.valueOf(contract.getAgreedAmount())
        );
        publish(ProposalCompletedEvent.ROUTING_KEY, event, proposal.getId());
    }

    public void publishProposalCancelled(Proposal proposal, String reason) {
        ProposalCancelledEvent event = new ProposalCancelledEvent(
                proposal.getId(),
                proposal.getJobId(),
                proposal.getFreelancerId(),
                reason
        );
        publish(ProposalCancelledEvent.ROUTING_KEY, event, proposal.getId());
    }

    public void publishProposalWithdrawn(Proposal proposal) {
        ProposalWithdrawnEvent event = new ProposalWithdrawnEvent(
                proposal.getId(),
                proposal.getJobId(),
                proposal.getFreelancerId()
        );
        publish(ProposalWithdrawnEvent.ROUTING_KEY, event, proposal.getId());
    }

    private void publish(String routingKey, Object event, Long proposalId) {
        MDC.put("routingKey", routingKey);
        MDC.put("proposalId", String.valueOf(proposalId));
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.PROPOSAL_EXCHANGE, routingKey, event);
            log.info("Published {} for proposalId={}", routingKey, proposalId);
        } catch (AmqpException e) {
            log.warn("Failed to publish {} for proposalId={}: {}", routingKey, proposalId, e.getMessage());
        } finally {
            MDC.remove("proposalId");
            MDC.remove("routingKey");
        }
    }
}
