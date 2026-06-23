package com.team01.freelance.proposal.messaging;

import com.team01.freelance.contracts.events.ContractCreatedEvent;
import com.team01.freelance.contracts.events.ContractStatusChangedEvent;
import com.team01.freelance.contracts.events.PaymentCompletedEvent;
import com.team01.freelance.contracts.events.PaymentFailedEvent;
import com.team01.freelance.contracts.events.PaymentInitiatedEvent;
import com.team01.freelance.contracts.events.PaymentRefundedEvent;
import com.team01.freelance.proposal.config.RabbitMQConfig;
import com.team01.freelance.proposal.service.ProposalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ProposalSagaConsumer {

    private static final Logger log = LoggerFactory.getLogger(ProposalSagaConsumer.class);

    private final ProposalService proposalService;

    public ProposalSagaConsumer(ProposalService proposalService) {
        this.proposalService = proposalService;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    public void handle(Object event) {
        try {
            if (event instanceof ContractCreatedEvent contractCreated) {
                proposalService.handleContractCreated(contractCreated);
            } else if (event instanceof ContractStatusChangedEvent contractStatusChanged) {
                proposalService.handleContractStatusChanged(contractStatusChanged);
            } else if (event instanceof PaymentInitiatedEvent paymentInitiated) {
                proposalService.handlePaymentInitiated(paymentInitiated);
            } else if (event instanceof PaymentCompletedEvent paymentCompleted) {
                proposalService.handlePaymentCompleted(paymentCompleted);
            } else if (event instanceof PaymentFailedEvent paymentFailed) {
                proposalService.handlePaymentFailed(paymentFailed);
            } else if (event instanceof PaymentRefundedEvent paymentRefunded) {
                proposalService.handlePaymentRefunded(paymentRefunded);
            } else {
                log.debug("Ignoring unsupported proposal-service saga event {}", event.getClass().getName());
            }
        } catch (RuntimeException e) {
            log.error("Failed to process proposal-service saga event: {}", e.getMessage());
            throw e;
        }
    }
}
