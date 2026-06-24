package com.team01.freelance.proposal.saga;

import static org.assertj.core.api.Assertions.assertThat;

import com.team01.freelance.contracts.events.PaymentFailedEvent;
import com.team01.freelance.contracts.events.PaymentRefundedEvent;
import com.team01.freelance.contracts.events.ProposalCancelledEvent;
import com.team01.freelance.proposal.config.RabbitMQConfig;
import com.team01.freelance.proposal.messaging.ProposalSagaConsumer;
import com.team01.freelance.proposal.model.Proposal;
import com.team01.freelance.proposal.model.ProposalStatus;
import com.team01.freelance.proposal.repository.ProposalRepository;
import com.team01.freelance.proposal.support.AbstractIntegrationTest;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

/**
 * [§8.6 Scenario D] Reaper-driven payout abandonment follows the standard compensation path.
 */
@Transactional
@TestPropertySource(properties = "saga.payout.abandon-after=PT5S")
class ScenarioDAbandonmentSagaIntegrationTest extends AbstractIntegrationTest {

    private static final Long JOB_ID = 99010L;
    private static final Long FREELANCER_ID = 66010L;
    private static final Long CONTRACT_ID = 77010L;
    private static final BigDecimal REFUND_AMOUNT = new BigDecimal("2000.00");

    @Autowired
    private PayoutAbandonmentReaper payoutAbandonmentReaper;

    @Autowired
    private ProposalSagaConsumer proposalSagaConsumer;

    @Autowired
    private ProposalRepository proposalRepository;

    @Autowired
    private CapturingRabbitTemplate rabbitTemplate;

    @BeforeEach
    void clearPublishedEvents() {
        rabbitTemplate.clear();
    }

    @Test
    void scenarioD_reaperPublishesAbandonedPaymentFailureAndProposalCompensatesToRefunded() {
        Proposal proposal = proposalRepository.saveAndFlush(paymentPendingProposal());

        payoutAbandonmentReaper.reapAbandonedPayouts();

        CapturingRabbitTemplate.SentMessage paymentMessage = rabbitTemplate.singleMessage(
                RabbitMQConfig.PAYMENT_EXCHANGE,
                PaymentFailedEvent.ROUTING_KEY);
        PaymentFailedEvent abandonedPayment = (PaymentFailedEvent) paymentMessage.payload();
        assertThat(abandonedPayment.payoutId()).isNull();
        assertThat(abandonedPayment.proposalId()).isEqualTo(proposal.getId());
        assertThat(abandonedPayment.contractId()).isEqualTo(CONTRACT_ID);
        assertThat(abandonedPayment.reason()).isEqualTo("payout_abandoned");

        proposalSagaConsumer.handle(abandonedPayment);

        Proposal failed = proposalRepository.findById(proposal.getId()).orElseThrow();
        assertThat(failed.getStatus()).isEqualTo(ProposalStatus.PAYMENT_FAILED);

        CapturingRabbitTemplate.SentMessage cancellationMessage = rabbitTemplate.singleMessage(
                RabbitMQConfig.PROPOSAL_EXCHANGE,
                ProposalCancelledEvent.ROUTING_KEY);
        ProposalCancelledEvent cancellation = (ProposalCancelledEvent) cancellationMessage.payload();
        assertThat(cancellation.proposalId()).isEqualTo(proposal.getId());
        assertThat(cancellation.jobId()).isEqualTo(JOB_ID);
        assertThat(cancellation.freelancerId()).isEqualTo(FREELANCER_ID);
        assertThat(cancellation.reason()).isEqualTo("payout_abandoned");

        proposalSagaConsumer.handle(new PaymentRefundedEvent(
                null,
                proposal.getId(),
                CONTRACT_ID,
                REFUND_AMOUNT));

        Proposal refunded = proposalRepository.findById(proposal.getId()).orElseThrow();
        assertThat(refunded.getStatus()).isEqualTo(ProposalStatus.REFUNDED);
    }

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
            // This test double never opens a broker connection.
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
                    .filter(message -> message.exchange().equals(exchange))
                    .filter(message -> message.routingKey().equals(routingKey))
                    .toList();
            assertThat(matches).hasSize(1);
            return matches.getFirst();
        }

        record SentMessage(String exchange, String routingKey, Object payload) {
        }
    }

    private static Proposal paymentPendingProposal() {
        Proposal proposal = new Proposal();
        proposal.setJobId(JOB_ID);
        proposal.setFreelancerId(FREELANCER_ID);
        proposal.setContractId(CONTRACT_ID);
        proposal.setCoverLetter("Scenario D payout abandonment");
        proposal.setBidAmount(REFUND_AMOUNT.doubleValue());
        proposal.setEstimatedDays(7);
        proposal.setStatus(ProposalStatus.PAYMENT_PENDING);
        proposal.setPaymentPendingAt(LocalDateTime.now().minusSeconds(6));
        proposal.setSubmittedAt(LocalDateTime.now().minusDays(1));
        return proposal;
    }
}
