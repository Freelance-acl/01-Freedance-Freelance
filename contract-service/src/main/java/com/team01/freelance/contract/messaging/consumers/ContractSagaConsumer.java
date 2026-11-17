package com.team01.freelance.contract.messaging.consumers;

import com.team01.freelance.contract.config.RabbitMQConfig;
import com.team01.freelance.contract.service.ContractService;
import com.team01.freelance.contracts.events.ProposalAcceptedEvent;
import com.team01.freelance.contracts.events.ProposalCancelledEvent;
import com.team01.freelance.contracts.events.ProposalCompletedEvent;
import com.team01.freelance.contracts.events.UserDeactivatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ContractSagaConsumer {

    private static final Logger log = LoggerFactory.getLogger(ContractSagaConsumer.class);

    private final ContractService contractService;

    public ContractSagaConsumer(ContractService contractService) {
        this.contractService = contractService;
    }

    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.beans.factory.annotation.Qualifier("jsonMessageConverter")
    private org.springframework.amqp.support.converter.MessageConverter messageConverter;

    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    public void handleMessage(org.springframework.amqp.core.Message message) {
        // Object parameters can match the framework's provided raw Message argument.
        // Decode explicitly so the dispatcher always receives the shared event record.
        handle(messageConverter.fromMessage(message));
    }

    public void handle(Object event) {
        try {
            if (event instanceof ProposalAcceptedEvent proposalAccepted) {
                contractService.handleProposalAccepted(proposalAccepted);
            } else if (event instanceof ProposalCompletedEvent proposalCompleted) {
                contractService.handleProposalCompleted(proposalCompleted);
            } else if (event instanceof ProposalCancelledEvent proposalCancelled) {
                contractService.handleProposalCancelled(proposalCancelled);
            } else if (event instanceof UserDeactivatedEvent userDeactivated) {
                contractService.handleUserDeactivated(userDeactivated);
            } else {
                log.debug("Ignoring unsupported contract-service event {}", event.getClass().getName());
            }
        } catch (RuntimeException e) {
            log.error("Failed to process contract-service saga event: {}", e.getMessage());
            throw e;
        }
    }
}
