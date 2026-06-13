package com.team01.freelance.proposal.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Section 2.9 topology for proposal-service (saga orchestrator).
 * Producer exchange: proposal.events.
 * Consumes user, job, contract and payment events via one saga-feedback queue + DLQ.
 */
@Configuration
public class RabbitMQConfig {

    public static final String PROPOSAL_EXCHANGE = "proposal.events";
    public static final String USER_EXCHANGE     = "user.events";
    public static final String JOB_EXCHANGE      = "job.events";
    public static final String CONTRACT_EXCHANGE = "contract.events";
    public static final String PAYMENT_EXCHANGE  = "payment.events";

    public static final String QUEUE = "proposal.saga-feedback";
    public static final String DLX   = "proposal.events.dlx";
    public static final String DLQ   = "proposal.saga-feedback.dlq";

    @Bean
    public TopicExchange proposalEventsExchange() {
        return new TopicExchange(PROPOSAL_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange userEventsExchange() {
        return new TopicExchange(USER_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange jobEventsExchange() {
        return new TopicExchange(JOB_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange contractEventsExchange() {
        return new TopicExchange(CONTRACT_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange paymentEventsExchange() {
        return new TopicExchange(PAYMENT_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(DLX, true, false);
    }

    @Bean
    public Queue consumerQueue() {
        return QueueBuilder.durable(QUEUE)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", DLQ)
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with(DLQ);
    }

    @Bean
    public Binding bindUserRegistered() {
        return BindingBuilder.bind(consumerQueue()).to(userEventsExchange()).with("user.registered");
    }

    @Bean
    public Binding bindUserDeactivated() {
        return BindingBuilder.bind(consumerQueue()).to(userEventsExchange()).with("user.deactivated");
    }

    @Bean
    public Binding bindJobStatusChanged() {
        return BindingBuilder.bind(consumerQueue()).to(jobEventsExchange()).with("job.status-changed");
    }

    @Bean
    public Binding bindJobRated() {
        return BindingBuilder.bind(consumerQueue()).to(jobEventsExchange()).with("job.rated");
    }

    @Bean
    public Binding bindJobClosed() {
        return BindingBuilder.bind(consumerQueue()).to(jobEventsExchange()).with("job.closed");
    }

    @Bean
    public Binding bindContractCreated() {
        return BindingBuilder.bind(consumerQueue()).to(contractEventsExchange()).with("contract.created");
    }

    @Bean
    public Binding bindContractStatusChanged() {
        return BindingBuilder.bind(consumerQueue()).to(contractEventsExchange()).with("contract.status-changed");
    }

    @Bean
    public Binding bindContractCancelled() {
        return BindingBuilder.bind(consumerQueue()).to(contractEventsExchange()).with("contract.cancelled");
    }

    @Bean
    public Binding bindPaymentInitiated() {
        return BindingBuilder.bind(consumerQueue()).to(paymentEventsExchange()).with("payment.initiated");
    }

    @Bean
    public Binding bindPaymentCompleted() {
        return BindingBuilder.bind(consumerQueue()).to(paymentEventsExchange()).with("payment.completed");
    }

    @Bean
    public Binding bindPaymentFailed() {
        return BindingBuilder.bind(consumerQueue()).to(paymentEventsExchange()).with("payment.failed");
    }

    @Bean
    public Binding bindPaymentRefunded() {
        return BindingBuilder.bind(consumerQueue()).to(paymentEventsExchange()).with("payment.refunded");
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new Jackson2JsonMessageConverter(mapper);
    }
}
