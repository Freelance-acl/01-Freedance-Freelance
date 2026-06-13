package com.team01.freelance.contract.config;

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
 * Section 2.9 topology for contract-service.
 * Producer exchange: contract.events.
 * Consumes user.deactivated, job.status-changed, proposal.accepted/completed/cancelled
 * via one saga-listener queue + DLQ.
 */
@Configuration
public class RabbitMQConfig {

    public static final String CONTRACT_EXCHANGE = "contract.events";
    public static final String USER_EXCHANGE     = "user.events";
    public static final String JOB_EXCHANGE      = "job.events";
    public static final String PROPOSAL_EXCHANGE = "proposal.events";

    public static final String QUEUE = "contract.saga-listener";
    public static final String DLX   = "contract.events.dlx";
    public static final String DLQ   = "contract.saga-listener.dlq";

    @Bean
    public TopicExchange contractEventsExchange() {
        return new TopicExchange(CONTRACT_EXCHANGE, true, false);
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
    public TopicExchange proposalEventsExchange() {
        return new TopicExchange(PROPOSAL_EXCHANGE, true, false);
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
    public Binding bindUserDeactivated() {
        return BindingBuilder.bind(consumerQueue()).to(userEventsExchange()).with("user.deactivated");
    }

    @Bean
    public Binding bindJobStatusChanged() {
        return BindingBuilder.bind(consumerQueue()).to(jobEventsExchange()).with("job.status-changed");
    }

    @Bean
    public Binding bindProposalAccepted() {
        return BindingBuilder.bind(consumerQueue()).to(proposalEventsExchange()).with("proposal.accepted");
    }

    @Bean
    public Binding bindProposalCompleted() {
        return BindingBuilder.bind(consumerQueue()).to(proposalEventsExchange()).with("proposal.completed");
    }

    @Bean
    public Binding bindProposalCancelled() {
        return BindingBuilder.bind(consumerQueue()).to(proposalEventsExchange()).with("proposal.cancelled");
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new Jackson2JsonMessageConverter(mapper);
    }
}
