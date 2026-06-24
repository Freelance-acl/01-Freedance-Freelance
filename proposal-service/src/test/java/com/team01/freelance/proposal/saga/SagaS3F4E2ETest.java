package com.team01.freelance.proposal.saga;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.team01.freelance.contracts.events.ContractCreatedEvent;
import com.team01.freelance.contracts.events.PaymentFailedEvent;
import com.team01.freelance.contracts.events.PaymentInitiatedEvent;
import com.team01.freelance.contracts.events.ProposalCancelledEvent;
import com.team01.freelance.contracts.events.ProposalCompletedEvent;
import com.team01.freelance.proposal.config.RabbitMQConfig;
import com.team01.freelance.proposal.feign.ContractServiceClient;
import com.team01.freelance.proposal.feign.JobServiceClient;
import com.team01.freelance.proposal.feign.UserServiceClient;
import com.team01.freelance.proposal.messaging.consumers.ProposalSagaConsumer;
import com.team01.freelance.proposal.model.Proposal;
import com.team01.freelance.proposal.model.ProposalStatus;
import com.team01.freelance.proposal.repository.ProposalRepository;
import com.team01.freelance.proposal.support.AbstractIntegrationTest;
import com.team01.freelance.proposal.support.FeignTestFixtures;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bonus §15 — (3) Saga S3-F4 end-to-end integration test.
 *
 * Drives the complete proposal-completion saga through proposal-service only:
 *
 *   ACCEPTED ──triggerCompletion()──► COMPLETING ──contract.created──►
 *   ──payment.initiated──► PAYMENT_PENDING ──payment.failed──► PAYMENT_FAILED
 *
 * Asserts that:
 *   1. {@code proposal.completed} is published (contract.service ingests this).
 *   2. {@code contract.created} → contractId linked on proposal.
 *   3. {@code payment.initiated} → proposal moves to PAYMENT_PENDING.
 *   4. Injected payment failure → PAYMENT_FAILED + {@code proposal.cancelled}
 *      (compensation) is published.
 */
@Transactional
class SagaS3F4E2ETest extends AbstractIntegrationTest {

    private static final Long JOB_ID        = 55001L;
    private static final Long FREELANCER_ID = 66001L;
    private static final Long CONTRACT_ID   = 77001L;
    private static final double BID_AMOUNT  = 3000.0;

    @MockitoBean protected JobServiceClient      jobServiceClient;
    @MockitoBean protected UserServiceClient     userServiceClient;
    @MockitoBean protected ContractServiceClient contractServiceClient;

    @Autowired private SagaTriggerService         sagaTriggerService;
    @Autowired private ProposalSagaConsumer       proposalSagaConsumer;
    @Autowired private ProposalRepository         proposalRepository;
    @Autowired private CapturingRabbitTemplate    rabbitTemplate;

    private Proposal proposal;

    @BeforeEach
    void setUp() {
        rabbitTemplate.clear();

        when(jobServiceClient.getJob(JOB_ID))
                .thenReturn(FeignTestFixtures.openJob(JOB_ID, 1L));
        when(userServiceClient.getUser(FREELANCER_ID))
                .thenReturn(FeignTestFixtures.activeFreelancer(FREELANCER_ID));
        when(contractServiceClient.getActiveContract(org.mockito.ArgumentMatchers.anyLong()))
                .thenReturn(FeignTestFixtures.activeContract(CONTRACT_ID, 0L, BID_AMOUNT));

        proposal = new Proposal();
        proposal.setJobId(JOB_ID);
        proposal.setFreelancerId(FREELANCER_ID);
        proposal.setCoverLetter("S3-F4 saga E2E test");
        proposal.setBidAmount(BID_AMOUNT);
        proposal.setEstimatedDays(14);
        proposal.setStatus(ProposalStatus.ACCEPTED);
        proposal.setSubmittedAt(LocalDateTime.now().minusDays(1));
        proposal = proposalRepository.saveAndFlush(proposal);
    }

    @Test
    void s3f4_fullSagaWithPaymentFailureCompensation() {

        // ── Step 1: trigger completion (S3-F4 entry point) ──────────────────
        Proposal completing = sagaTriggerService.triggerCompletion(proposal.getId());

        assertThat(completing.getStatus()).isEqualTo(ProposalStatus.COMPLETING);

        // Assert proposal.completed was published → contract-service will pick it up
        CapturingRabbitTemplate.SentMessage completedMsg = rabbitTemplate.singleMessage(
                RabbitMQConfig.PROPOSAL_EXCHANGE, ProposalCompletedEvent.ROUTING_KEY);
        ProposalCompletedEvent completedEvent = (ProposalCompletedEvent) completedMsg.payload();
        assertThat(completedEvent.proposalId()).isEqualTo(proposal.getId());
        assertThat(completedEvent.contractId()).isEqualTo(CONTRACT_ID);
        assertThat(completedEvent.agreedAmount()).isEqualByComparingTo(BigDecimal.valueOf(BID_AMOUNT));

        // ── Step 2: contract.created arrives (contract-service published this) ─
        proposalSagaConsumer.handle(new ContractCreatedEvent(
                CONTRACT_ID, proposal.getId(), JOB_ID, FREELANCER_ID,
                BigDecimal.valueOf(BID_AMOUNT)));

        Proposal afterContract = proposalRepository.findById(proposal.getId()).orElseThrow();
        assertThat(afterContract.getContractId()).isEqualTo(CONTRACT_ID);

        // ── Step 3: payment.initiated arrives (wallet-service published this) ─
        proposalSagaConsumer.handle(new PaymentInitiatedEvent(
                null, proposal.getId(), CONTRACT_ID, BigDecimal.valueOf(BID_AMOUNT)));

        Proposal paymentPending = proposalRepository.findById(proposal.getId()).orElseThrow();
        assertThat(paymentPending.getStatus()).isEqualTo(ProposalStatus.PAYMENT_PENDING);
        assertThat(paymentPending.getPaymentPendingAt()).isNotNull();

        // ── Step 4: inject payment.failed → compensation path ────────────────
        rabbitTemplate.clear();
        proposalSagaConsumer.handle(new PaymentFailedEvent(
                null, proposal.getId(), CONTRACT_ID, "card_declined"));

        Proposal compensated = proposalRepository.findById(proposal.getId()).orElseThrow();
        assertThat(compensated.getStatus()).isEqualTo(ProposalStatus.PAYMENT_FAILED);

        // Assert proposal.cancelled (compensation event) was published
        CapturingRabbitTemplate.SentMessage cancelledMsg = rabbitTemplate.singleMessage(
                RabbitMQConfig.PROPOSAL_EXCHANGE, ProposalCancelledEvent.ROUTING_KEY);
        ProposalCancelledEvent cancelledEvent = (ProposalCancelledEvent) cancelledMsg.payload();
        assertThat(cancelledEvent.proposalId()).isEqualTo(proposal.getId());
        assertThat(cancelledEvent.jobId()).isEqualTo(JOB_ID);
        assertThat(cancelledEvent.freelancerId()).isEqualTo(FREELANCER_ID);
        assertThat(cancelledEvent.reason()).isEqualTo("card_declined");
    }

    // ── Capturing RabbitTemplate ─────────────────────────────────────────────

    @TestConfiguration
    static class RabbitTemplateCaptureConfig {
        @Bean
        @Primary
        CapturingRabbitTemplate capturingRabbitTemplate() {
            return new CapturingRabbitTemplate();
        }
    }

    static class CapturingRabbitTemplate extends RabbitTemplate {

        private final List<SentMessage> messages = new CopyOnWriteArrayList<>();

        @Override
        public void afterPropertiesSet() {
            // Test double — never opens a real broker connection.
        }

        @Override
        public void convertAndSend(String exchange, String routingKey, Object object) throws AmqpException {
            messages.add(new SentMessage(exchange, routingKey, object));
        }

        void clear() {
            messages.clear();
        }

        SentMessage singleMessage(String exchange, String routingKey) {
            List<SentMessage> matches = messages.stream()
                    .filter(m -> m.exchange().equals(exchange) && m.routingKey().equals(routingKey))
                    .toList();
            assertThat(matches)
                    .as("Expected exactly one message on exchange=%s routingKey=%s", exchange, routingKey)
                    .hasSize(1);
            return matches.getFirst();
        }

        record SentMessage(String exchange, String routingKey, Object payload) {}
    }
}
