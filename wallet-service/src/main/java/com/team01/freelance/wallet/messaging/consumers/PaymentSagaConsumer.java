package com.team01.freelance.wallet.messaging.consumers;

import com.team01.freelance.contracts.events.ProposalCompletedEvent;
import com.team01.freelance.contracts.events.ProposalCancelledEvent;
import com.team01.freelance.wallet.config.RabbitMQConfig;
import com.team01.freelance.wallet.service.PayoutService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentSagaConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentSagaConsumer.class);

    private final PayoutService payoutService;

    public PaymentSagaConsumer(PayoutService payoutService) {
        this.payoutService = payoutService;
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
            if (event instanceof ProposalCompletedEvent proposalCompleted) {
                payoutService.handleProposalCompleted(proposalCompleted);
            } else if (event instanceof ProposalCancelledEvent proposalCancelled) {
                log.info("Received ProposalCancelledEvent for proposal {}", proposalCancelled.proposalId());

                payoutService.handleProposalCancelled(
                        proposalCancelled.proposalId(),
                        proposalCancelled.reason() != null ? proposalCancelled.reason() : "Saga: Proposal Cancelled"
                );
            } else {
                log.debug("Ignoring unsupported wallet-service saga event {}", event.getClass().getName());
            }
        } catch (RuntimeException e) {
            log.error("Failed to process wallet-service saga event: {}", e.getMessage());
            throw e;
        }
    }
}