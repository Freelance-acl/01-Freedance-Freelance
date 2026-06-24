package com.team01.freelance.proposal.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import com.team01.freelance.contracts.events.ContractCreatedEvent;
import com.team01.freelance.contracts.events.PaymentFailedEvent;
import com.team01.freelance.contracts.events.PaymentInitiatedEvent;
import com.team01.freelance.proposal.config.RabbitMQConfig;
import com.team01.freelance.proposal.feign.ContractServiceClient;
import com.team01.freelance.proposal.feign.JobServiceClient;
import com.team01.freelance.proposal.feign.UserServiceClient;
import com.team01.freelance.proposal.feign.WalletServiceClient;
import com.team01.freelance.proposal.messaging.consumers.ProposalSagaConsumer;
import com.team01.freelance.proposal.model.Proposal;
import com.team01.freelance.proposal.model.ProposalStatus;
import com.team01.freelance.proposal.repository.ProposalRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Bonus §15 — (2) RabbitMQ consumer integration test.
 *
 * Uses a real Testcontainers RabbitMQ broker.  Each test:
 *   1. Publishes an event to the broker exchange — verifying the AMQP topology
 *      (exchange → binding → queue) is correctly wired.
 *   2. Receives the raw AMQP Message back from the queue — proving the message
 *      was actually routed and stored by the broker.
 *   3. Converts the raw bytes back to the domain event via the same
 *      {@link Jackson2JsonMessageConverter} used in production.
 *   4. Invokes the saga consumer directly with the deserialized event and asserts
 *      the consumer mutates the local H2 database as expected.
 *
 * The background {@code @RabbitListener} is intentionally kept stopped so the
 * message remains in the queue for step 2.
 */
@SpringBootTest(properties = "spring.cloud.openfeign.circuitbreaker.enabled=false")
@ActiveProfiles("test")
@Testcontainers
class RabbitMQConsumerLiveTest {

    private static final long RECEIVE_TIMEOUT_MS = 5_000L;

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbitMQ =
            new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    @MockitoBean UserServiceClient     userServiceClient;
    @MockitoBean JobServiceClient      jobServiceClient;
    @MockitoBean ContractServiceClient contractServiceClient;
    @MockitoBean WalletServiceClient   walletServiceClient;

    @Autowired private RabbitTemplate                 rabbitTemplate;
    @Autowired private Jackson2JsonMessageConverter   messageConverter;
    @Autowired private ProposalSagaConsumer           proposalSagaConsumer;
    @Autowired private ProposalRepository             proposalRepository;

    private Proposal proposal;

    @BeforeEach
    void setUp() {
        proposal = new Proposal();
        proposal.setJobId(10L);
        proposal.setFreelancerId(20L);
        proposal.setCoverLetter("Live RabbitMQ consumer test");
        proposal.setBidAmount(1000.0);
        proposal.setEstimatedDays(5);
        proposal.setStatus(ProposalStatus.ACCEPTED);
        proposal.setSubmittedAt(LocalDateTime.now());
        proposal = proposalRepository.saveAndFlush(proposal);
    }

    @AfterEach
    void tearDown() {
        proposalRepository.deleteById(proposal.getId());
        // drain any leftover messages so queues stay clean between tests
        while (rabbitTemplate.receive(RabbitMQConfig.QUEUE) != null) { /* drain */ }
    }

    // ── helper: publish, receive from broker, convert, dispatch ─────────────

    /**
     * Publish {@code event} to {@code exchange}/{@code routingKey}, pull the
     * raw AMQP message back from the broker queue, convert it, and hand it to
     * the consumer.  This verifies the full broker-routing path + converter +
     * consumer in one round-trip.
     */
    private void publishReceiveAndDispatch(String exchange, String routingKey, Object event) {
        // Step 1 — publish to real broker
        rabbitTemplate.convertAndSend(exchange, routingKey, event);

        // Step 2 — receive raw bytes from broker (proves the exchange→queue binding works)
        Message raw = rabbitTemplate.receive(RabbitMQConfig.QUEUE, RECEIVE_TIMEOUT_MS);
        assertThat(raw).as("message should have been routed to %s queue", RabbitMQConfig.QUEUE)
                .isNotNull();

        // Step 3 — convert via the production Jackson converter (fromMessage without hint
        //          so the __TypeId__ header is used to determine the concrete event type)
        Object deserialized = messageConverter.fromMessage(raw);

        // Step 4 — hand to the consumer; asserts that the converter produced the right type
        proposalSagaConsumer.handle(deserialized);
    }

    // ── (a) contract.created → consumer links contractId ────────────────────

    @Test
    void publishContractCreated_consumerLinksContractIdOnProposal() {
        ContractCreatedEvent event = new ContractCreatedEvent(
                888L, proposal.getId(), proposal.getJobId(), proposal.getFreelancerId(),
                BigDecimal.valueOf(1000));

        publishReceiveAndDispatch(
                RabbitMQConfig.CONTRACT_EXCHANGE, ContractCreatedEvent.ROUTING_KEY, event);

        Proposal updated = proposalRepository.findById(proposal.getId()).orElseThrow();
        assertThat(updated.getContractId()).isEqualTo(888L);
    }

    // ── (b) payment.initiated → consumer moves to PAYMENT_PENDING ───────────

    @Test
    void publishPaymentInitiated_consumerMovesProposalToPaymentPending() {
        proposal.setStatus(ProposalStatus.COMPLETING);
        proposalRepository.saveAndFlush(proposal);

        PaymentInitiatedEvent event = new PaymentInitiatedEvent(
                1L, proposal.getId(), 900L, BigDecimal.valueOf(1000));

        publishReceiveAndDispatch(
                RabbitMQConfig.PAYMENT_EXCHANGE, PaymentInitiatedEvent.ROUTING_KEY, event);

        Proposal updated = proposalRepository.findById(proposal.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ProposalStatus.PAYMENT_PENDING);
        assertThat(updated.getPaymentPendingAt()).isNotNull();
    }

    // ── (c) payment.failed → consumer compensates to PAYMENT_FAILED ─────────

    @Test
    void publishPaymentFailed_consumerCompensatesToPaymentFailed() {
        proposal.setStatus(ProposalStatus.PAYMENT_PENDING);
        proposal.setPaymentPendingAt(LocalDateTime.now());
        proposal.setContractId(900L);
        proposalRepository.saveAndFlush(proposal);

        PaymentFailedEvent event = new PaymentFailedEvent(
                2L, proposal.getId(), 900L, "insufficient_funds");

        publishReceiveAndDispatch(
                RabbitMQConfig.PAYMENT_EXCHANGE, PaymentFailedEvent.ROUTING_KEY, event);

        Proposal updated = proposalRepository.findById(proposal.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ProposalStatus.PAYMENT_FAILED);
    }
}
