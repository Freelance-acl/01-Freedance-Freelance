package com.team01.freelance.job.config;

import com.team01.freelance.contracts.events.ProposalAcceptedEvent;
import com.team01.freelance.contracts.events.ProposalCancelledEvent;
import com.team01.freelance.contracts.events.ProposalCompletedEvent;
import com.team01.freelance.contracts.events.ProposalWithdrawnEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * §2.9 topology for job-service.
 * Producer exchange: job.events. Consumes proposal.* via one saga-listener queue + DLQ.
 */
@Configuration
public class RabbitMQConfig {

    public static final String JOB_EXCHANGE      = "job.events";
    public static final String PROPOSAL_EXCHANGE = "proposal.events";

    public static final String QUEUE = "job.proposal.saga-listener";
    public static final String DLX   = "job.events.dlx";
    public static final String DLQ   = "job.proposal.saga-listener.dlq";

    /** Package allowlist for inbound saga event deserialization (see application.yml). */
    public static final String TRUSTED_EVENT_PACKAGE = "com.team01.freelance.contracts.events";

    @Value("${job-service.rabbitmq.trusted-event-package}")
    private String trustedEventPackage;

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
    public Binding bindProposalWithdrawn() {
        return BindingBuilder.bind(consumerQueue()).to(proposalEventsExchange()).with("proposal.withdrawn");
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTrustedPackages(trustedEventPackage);
        typeMapper.setIdClassMapping(proposalEventTypeMapping());

        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(mapper);
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }

    private static Map<String, Class<?>> proposalEventTypeMapping() {
        Map<String, Class<?>> mappings = new LinkedHashMap<>();
        mappings.put(ProposalAcceptedEvent.class.getName(), ProposalAcceptedEvent.class);
        mappings.put(ProposalAcceptedEvent.ROUTING_KEY, ProposalAcceptedEvent.class);
        mappings.put(ProposalCompletedEvent.class.getName(), ProposalCompletedEvent.class);
        mappings.put(ProposalCompletedEvent.ROUTING_KEY, ProposalCompletedEvent.class);
        mappings.put(ProposalCancelledEvent.class.getName(), ProposalCancelledEvent.class);
        mappings.put(ProposalCancelledEvent.ROUTING_KEY, ProposalCancelledEvent.class);
        mappings.put(ProposalWithdrawnEvent.class.getName(), ProposalWithdrawnEvent.class);
        mappings.put(ProposalWithdrawnEvent.ROUTING_KEY, ProposalWithdrawnEvent.class);
        return Map.copyOf(mappings);
    }
}
