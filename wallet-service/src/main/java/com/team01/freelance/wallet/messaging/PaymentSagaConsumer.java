package com.team01.freelance.wallet.messaging;

import com.team01.freelance.contracts.events.ProposalCompletedEvent;
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

    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    public void handle(Object event) {
        try {
            if (event instanceof ProposalCompletedEvent proposalCompleted) {
                payoutService.handleProposalCompleted(proposalCompleted);
            } else {
                log.debug("Ignoring unsupported wallet-service saga event {}", event.getClass().getName());
            }
        } catch (RuntimeException e) {
            log.error("Failed to process wallet-service saga event: {}", e.getMessage());
            throw e;
        }
    }
}
