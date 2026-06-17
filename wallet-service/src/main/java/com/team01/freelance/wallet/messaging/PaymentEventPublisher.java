package com.team01.freelance.wallet.messaging;

import com.team01.freelance.contracts.events.PaymentCompletedEvent;
import com.team01.freelance.contracts.events.PaymentFailedEvent;
import com.team01.freelance.contracts.events.PaymentInitiatedEvent;
import com.team01.freelance.contracts.events.PaymentRefundedEvent;
import com.team01.freelance.wallet.config.RabbitMQConfig;
import com.team01.freelance.wallet.model.Payout;
import java.math.BigDecimal;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public PaymentEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishPaymentInitiated(Payout payout, Long proposalId) {
        PaymentInitiatedEvent event = new PaymentInitiatedEvent(
                payout.getId(),
                proposalId,
                payout.getContractId(),
                BigDecimal.valueOf(payout.getAmount())
        );
        rabbitTemplate.convertAndSend(RabbitMQConfig.PAYMENT_EXCHANGE, PaymentInitiatedEvent.ROUTING_KEY, event);
    }

    public void publishPaymentCompleted(Payout payout, Long proposalId) {
        PaymentCompletedEvent event = new PaymentCompletedEvent(
                payout.getId(),
                proposalId,
                payout.getContractId(),
                BigDecimal.valueOf(payout.getAmount())
        );
        rabbitTemplate.convertAndSend(RabbitMQConfig.PAYMENT_EXCHANGE, PaymentCompletedEvent.ROUTING_KEY, event);
    }

    public void publishPaymentFailed(Payout payout, Long proposalId, String reason) {
        PaymentFailedEvent event = new PaymentFailedEvent(
                payout.getId(),
                proposalId,
                payout.getContractId(),
                reason
        );
        rabbitTemplate.convertAndSend(RabbitMQConfig.PAYMENT_EXCHANGE, PaymentFailedEvent.ROUTING_KEY, event);
    }

    public void publishPaymentRefunded(Payout payout, Long proposalId, BigDecimal refundAmount) {
        PaymentRefundedEvent event = new PaymentRefundedEvent(
                payout.getId(),
                proposalId,
                payout.getContractId(),
                refundAmount
        );
        rabbitTemplate.convertAndSend(RabbitMQConfig.PAYMENT_EXCHANGE, PaymentRefundedEvent.ROUTING_KEY, event);
    }
}
