package com.team01.freelance.proposal.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import com.team01.freelance.contracts.events.ContractCreatedEvent;
import com.team01.freelance.contracts.events.ContractStatusChangedEvent;
import com.team01.freelance.contracts.events.PaymentCompletedEvent;
import com.team01.freelance.contracts.events.PaymentFailedEvent;
import com.team01.freelance.contracts.events.PaymentInitiatedEvent;
import com.team01.freelance.contracts.events.PaymentRefundedEvent;
import com.team01.freelance.proposal.model.Proposal;
import com.team01.freelance.proposal.model.ProposalStatus;
import com.team01.freelance.proposal.repository.ProposalRepository;
import com.team01.freelance.proposal.support.AbstractIntegrationTest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class ProposalSagaConsumerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ProposalSagaConsumer proposalSagaConsumer;

    @Autowired
    private ProposalRepository proposalRepository;

    @Autowired
    private CapturingProposalEventPublisher proposalEventPublisher;

    private Proposal proposal;

    @BeforeEach
    void setUp() {
        proposalEventPublisher.clear();
        proposal = new Proposal();
        proposal.setJobId(100L);
        proposal.setFreelancerId(200L);
        proposal.setCoverLetter("Saga consumer test");
        proposal.setBidAmount(1500.0);
        proposal.setEstimatedDays(10);
        proposal.setStatus(ProposalStatus.ACCEPTED);
        proposal.setSubmittedAt(LocalDateTime.now());
        proposal = proposalRepository.save(proposal);
    }

    @Test
    void handleContractCreated_linksContractIdOnProposal() {
        proposalSagaConsumer.handle(new ContractCreatedEvent(
                900L, proposal.getId(), proposal.getJobId(), proposal.getFreelancerId(), BigDecimal.valueOf(1500)));

        Proposal updated = proposalRepository.findById(proposal.getId()).orElseThrow();
        assertThat(updated.getContractId()).isEqualTo(900L);
    }

    @Test
    void handleContractStatusChanged_linksContractIdWhenAlreadyStored() {
        proposal.setContractId(901L);
        proposalRepository.saveAndFlush(proposal);

        proposalSagaConsumer.handle(new ContractStatusChangedEvent(901L, "ACTIVE", "COMPLETED"));

        Proposal updated = proposalRepository.findById(proposal.getId()).orElseThrow();
        assertThat(updated.getContractId()).isEqualTo(901L);
    }

    @Test
    void handlePaymentInitiated_movesCompletingProposalToPaymentPending() {
        proposal.setStatus(ProposalStatus.COMPLETING);
        proposalRepository.saveAndFlush(proposal);

        proposalSagaConsumer.handle(new PaymentInitiatedEvent(
                1L, proposal.getId(), 901L, BigDecimal.valueOf(1500)));

        Proposal updated = proposalRepository.findById(proposal.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ProposalStatus.PAYMENT_PENDING);
        assertThat(updated.getPaymentPendingAt()).isNotNull();
        assertThat(updated.getContractId()).isEqualTo(901L);
    }

    @Test
    void handlePaymentFailed_marksFailedAndPublishesCancellation() {
        proposal.setStatus(ProposalStatus.PAYMENT_PENDING);
        proposal.setPaymentPendingAt(LocalDateTime.now());
        proposal.setContractId(901L);
        proposalRepository.saveAndFlush(proposal);

        proposalSagaConsumer.handle(new PaymentFailedEvent(
                2L, proposal.getId(), 901L, "payment_failed"));

        Proposal updated = proposalRepository.findById(proposal.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ProposalStatus.PAYMENT_FAILED);
        assertThat(proposalEventPublisher.cancelledEvents()).containsExactly(
                new CapturingProposalEventPublisher.CancelledEvent(updated.getId(), "payment_failed"));
    }

    @Test
    void handlePaymentCompleted_movesPaymentPendingProposalToPaid() {
        proposal.setStatus(ProposalStatus.PAYMENT_PENDING);
        proposal.setPaymentPendingAt(LocalDateTime.now());
        proposal.setContractId(901L);
        proposalRepository.saveAndFlush(proposal);

        proposalSagaConsumer.handle(new PaymentCompletedEvent(
                3L, proposal.getId(), 901L, BigDecimal.valueOf(1500)));

        Proposal updated = proposalRepository.findById(proposal.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ProposalStatus.PAID);
    }

    @Test
    void handlePaymentRefunded_movesPaymentFailedProposalToRefunded() {
        proposal.setStatus(ProposalStatus.PAYMENT_FAILED);
        proposal.setContractId(901L);
        proposalRepository.saveAndFlush(proposal);

        proposalSagaConsumer.handle(new PaymentRefundedEvent(
                4L, proposal.getId(), 901L, BigDecimal.valueOf(1500)));

        Proposal updated = proposalRepository.findById(proposal.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ProposalStatus.REFUNDED);
    }

    @Test
    void scenarioD_abandonedPayoutReasonMarksFailedAndPublishesCancellation() {
        proposal.setStatus(ProposalStatus.PAYMENT_PENDING);
        proposal.setPaymentPendingAt(LocalDateTime.now().minusHours(80));
        proposal.setContractId(901L);
        proposalRepository.saveAndFlush(proposal);

        proposalSagaConsumer.handle(new PaymentFailedEvent(
                5L, proposal.getId(), 901L, "payout_abandoned"));

        Proposal updated = proposalRepository.findById(proposal.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ProposalStatus.PAYMENT_FAILED);
        assertThat(proposalEventPublisher.cancelledEvents()).containsExactly(
                new CapturingProposalEventPublisher.CancelledEvent(updated.getId(), "payout_abandoned"));
    }

    @TestConfiguration
    static class ProposalEventPublisherCaptureConfig {

        @Bean
        @Primary
        CapturingProposalEventPublisher capturingProposalEventPublisher() {
            return new CapturingProposalEventPublisher();
        }
    }

    static class CapturingProposalEventPublisher extends ProposalEventPublisher {

        private final List<CancelledEvent> cancelledEvents = new ArrayList<>();

        CapturingProposalEventPublisher() {
            super(new RabbitTemplate());
        }

        @Override
        public void publishProposalCancelled(Proposal proposal, String reason) {
            cancelledEvents.add(new CancelledEvent(proposal.getId(), reason));
        }

        void clear() {
            cancelledEvents.clear();
        }

        List<CancelledEvent> cancelledEvents() {
            return cancelledEvents;
        }

        record CancelledEvent(Long proposalId, String reason) {
        }
    }
}
