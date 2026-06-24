package com.team01.freelance.proposal.messaging.publishers;

import com.team01.freelance.contracts.events.PaymentFailedEvent;
import com.team01.freelance.proposal.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public PaymentEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishPaymentFailed(Long payoutId, Long proposalId, Long contractId, String reason) {
        PaymentFailedEvent event = new PaymentFailedEvent(payoutId, proposalId, contractId, reason);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.PAYMENT_EXCHANGE,
                PaymentFailedEvent.ROUTING_KEY,
                event
        );
    }
}
