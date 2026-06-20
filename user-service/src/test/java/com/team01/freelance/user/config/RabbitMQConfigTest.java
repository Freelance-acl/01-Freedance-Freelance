package com.team01.freelance.user.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RabbitMQConfigTest {

    private final RabbitMQConfig config = new RabbitMQConfig();

    @Test
    void declaresUserEventsTopicExchange() {
        TopicExchange exchange = config.userEventsExchange();

        assertEquals(RabbitMQConfig.USER_EXCHANGE, exchange.getName());
        assertTrue(exchange.isDurable());
        assertFalse(exchange.isAutoDelete());
    }

    @Test
    void declaresProposalEventsTopicExchange() {
        TopicExchange exchange = config.proposalEventsExchange();

        assertEquals(RabbitMQConfig.PROPOSAL_EXCHANGE, exchange.getName());
        assertTrue(exchange.isDurable());
        assertFalse(exchange.isAutoDelete());
    }

    @Test
    void declaresConsumerQueueWithDeadLetterArguments() {
        Queue queue = config.consumerQueue();

        assertEquals(RabbitMQConfig.QUEUE, queue.getName());
        assertTrue(queue.isDurable());
        assertEquals(RabbitMQConfig.DLX, queue.getArguments().get("x-dead-letter-exchange"));
        assertEquals(RabbitMQConfig.DLQ, queue.getArguments().get("x-dead-letter-routing-key"));
    }

    @Test
    void declaresDeadLetterQueueAndBinding() {
        Queue deadLetterQueue = config.deadLetterQueue();
        Binding binding = config.deadLetterBinding();

        assertEquals(RabbitMQConfig.DLQ, deadLetterQueue.getName());
        assertTrue(deadLetterQueue.isDurable());

        assertEquals(RabbitMQConfig.DLQ, binding.getDestination());
        assertEquals(RabbitMQConfig.DLX, binding.getExchange());
        assertEquals(RabbitMQConfig.DLQ, binding.getRoutingKey());
    }

    @Test
    void bindsProposalCompletedAndCancelledToSagaListenerQueue() {
        Binding completedBinding = config.bindProposalCompleted();
        Binding cancelledBinding = config.bindProposalCancelled();

        assertEquals(RabbitMQConfig.QUEUE, completedBinding.getDestination());
        assertEquals(RabbitMQConfig.PROPOSAL_EXCHANGE, completedBinding.getExchange());
        assertEquals("proposal.completed", completedBinding.getRoutingKey());

        assertEquals(RabbitMQConfig.QUEUE, cancelledBinding.getDestination());
        assertEquals(RabbitMQConfig.PROPOSAL_EXCHANGE, cancelledBinding.getExchange());
        assertEquals("proposal.cancelled", cancelledBinding.getRoutingKey());
    }

    @Test
    void declaresJsonMessageConverter() {
        Object converter = config.jsonMessageConverter();

        assertNotNull(converter);
        assertEquals("Jackson2JsonMessageConverter", converter.getClass().getSimpleName());
    }
}
