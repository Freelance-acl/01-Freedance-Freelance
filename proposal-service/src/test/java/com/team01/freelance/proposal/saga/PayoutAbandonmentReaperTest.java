package com.team01.freelance.proposal.saga;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.team01.freelance.proposal.messaging.publishers.PaymentEventPublisher;
import com.team01.freelance.proposal.model.Proposal;
import com.team01.freelance.proposal.model.ProposalStatus;
import com.team01.freelance.proposal.repository.ProposalRepository;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class PayoutAbandonmentReaperTest {

    private ProposalRepository proposalRepository;

    private CapturingPaymentEventPublisher paymentEventPublisher;

    @BeforeEach
    void setUp() {
        paymentEventPublisher = new CapturingPaymentEventPublisher();
    }

    @Test
    void reapAbandonedPayouts_publishesSyntheticPaymentFailed() {
        Proposal proposal = new Proposal();
        proposal.setId(44L);
        proposal.setContractId(55L);
        proposal.setStatus(ProposalStatus.PAYMENT_PENDING);
        proposal.setPaymentPendingAt(LocalDateTime.now().minusSeconds(10));
        proposalRepository = repositoryReturning(List.of(proposal));

        PayoutAbandonmentReaper shortReaper = new PayoutAbandonmentReaper(
                proposalRepository, paymentEventPublisher, Duration.ofSeconds(5));

        shortReaper.reapAbandonedPayouts();

        assertThat(paymentEventPublisher.publishedFailures()).containsExactly(
                new CapturingPaymentEventPublisher.PublishedFailure(null, 44L, 55L, "payout_abandoned"));
    }

    @Test
    void stateMachine_rejectsInvalidTransition() {
        ProposalStateMachine machine = new ProposalStateMachine();
        Proposal proposal = new Proposal();
        proposal.setStatus(ProposalStatus.ACCEPTED);

        assertThatThrownBy(() -> machine.transition(proposal, ProposalStatus.PAID))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void stateMachine_allowsHappyPathTransitions() {
        ProposalStateMachine machine = new ProposalStateMachine();
        Proposal proposal = new Proposal();
        proposal.setStatus(ProposalStatus.ACCEPTED);

        machine.transition(proposal, ProposalStatus.COMPLETING);
        machine.transition(proposal, ProposalStatus.PAYMENT_PENDING);
        machine.transition(proposal, ProposalStatus.PAID);

        assertThat(proposal.getStatus()).isEqualTo(ProposalStatus.PAID);
        assertThat(proposal.getPaymentPendingAt()).isNotNull();
    }

    private static ProposalRepository repositoryReturning(List<Proposal> abandonedProposals) {
        return (ProposalRepository) Proxy.newProxyInstance(
                ProposalRepository.class.getClassLoader(),
                new Class<?>[] { ProposalRepository.class },
                (proxy, method, args) -> {
                    if ("findByStatusAndPaymentPendingAtBefore".equals(method.getName())) {
                        assertThat(args[0]).isEqualTo(ProposalStatus.PAYMENT_PENDING);
                        assertThat(args[1]).isInstanceOf(LocalDateTime.class);
                        return abandonedProposals;
                    }
                    throw new UnsupportedOperationException("Unexpected repository call: " + method.getName());
                });
    }

    private static class CapturingPaymentEventPublisher extends PaymentEventPublisher {

        private final List<PublishedFailure> publishedFailures = new java.util.ArrayList<>();

        CapturingPaymentEventPublisher() {
            super(new RabbitTemplate());
        }

        @Override
        public void publishPaymentFailed(Long payoutId, Long proposalId, Long contractId, String reason) {
            publishedFailures.add(new PublishedFailure(payoutId, proposalId, contractId, reason));
        }

        List<PublishedFailure> publishedFailures() {
            return publishedFailures;
        }

        record PublishedFailure(Long payoutId, Long proposalId, Long contractId, String reason) {
        }
    }
}
