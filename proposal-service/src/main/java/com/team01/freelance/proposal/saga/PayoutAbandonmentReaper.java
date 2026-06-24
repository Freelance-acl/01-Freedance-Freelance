package com.team01.freelance.proposal.saga;

import com.team01.freelance.proposal.messaging.publishers.PaymentEventPublisher;
import com.team01.freelance.proposal.model.Proposal;
import com.team01.freelance.proposal.model.ProposalStatus;
import com.team01.freelance.proposal.repository.ProposalRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PayoutAbandonmentReaper {

    private static final Logger log = LoggerFactory.getLogger(PayoutAbandonmentReaper.class);
    private static final String ABANDON_REASON = "payout_abandoned";

    private final ProposalRepository proposalRepository;
    private final PaymentEventPublisher paymentEventPublisher;
    private final Duration abandonAfter;

    public PayoutAbandonmentReaper(
            ProposalRepository proposalRepository,
            PaymentEventPublisher paymentEventPublisher,
            @Value("${saga.payout.abandon-after:PT72H}") Duration abandonAfter) {
        this.proposalRepository = proposalRepository;
        this.paymentEventPublisher = paymentEventPublisher;
        this.abandonAfter = abandonAfter;
    }

    @Scheduled(fixedDelayString = "PT15M")
    public void reapAbandonedPayouts() {
        LocalDateTime cutoff = LocalDateTime.now().minus(abandonAfter);
        List<Proposal> abandoned = proposalRepository.findByStatusAndPaymentPendingAtBefore(
                ProposalStatus.PAYMENT_PENDING, cutoff);

        for (Proposal proposal : abandoned) {
            MDC.put("proposalId", String.valueOf(proposal.getId()));
            try {
                log.warn("Abandoning payout for proposal {} after {}", proposal.getId(), abandonAfter);
                paymentEventPublisher.publishPaymentFailed(
                        null,
                        proposal.getId(),
                        proposal.getContractId(),
                        ABANDON_REASON);
            } finally {
                MDC.remove("proposalId");
            }
        }
    }
}
