package com.team01.freelance.contract.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContractEventConfigTest {

    private final RabbitMQConfig config = new RabbitMQConfig();

    @Test
    void sagaQueueUsesDeadLetterExchange() {
        Queue queue = config.consumerQueue();
        Map<String, Object> arguments = queue.getArguments();

        assertEquals(RabbitMQConfig.QUEUE, queue.getName());
        assertEquals(RabbitMQConfig.DLX, arguments.get("x-dead-letter-exchange"));
        assertEquals(RabbitMQConfig.DLQ, arguments.get("x-dead-letter-routing-key"));
    }

    @Test
    void proposalBindingsUseExpectedRoutingKeys() {
        Binding accepted = config.bindProposalAccepted();
        Binding completed = config.bindProposalCompleted();
        Binding cancelled = config.bindProposalCancelled();

        assertEquals("proposal.accepted", accepted.getRoutingKey());
        assertEquals("proposal.completed", completed.getRoutingKey());
        assertEquals("proposal.cancelled", cancelled.getRoutingKey());
    }
}
